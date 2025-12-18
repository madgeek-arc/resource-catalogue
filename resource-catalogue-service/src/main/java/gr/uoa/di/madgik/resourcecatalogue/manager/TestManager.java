package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import org.springframework.security.core.Authentication;

@org.springframework.stereotype.Service("testManager")
public abstract class TestManager<R> {

    private final GenericResourceService genericResourceService;
    private final SecurityService securityService;
    private final CatalogueService catalogueService;

    protected abstract String getResourceTypeName();

    public TestManager(GenericResourceService genericResourceService,
                       SecurityService securityService,
                       CatalogueService catalogueService) {
        this.genericResourceService = genericResourceService;
        this.securityService = securityService;
        this.catalogueService = catalogueService;
    }

    public R get(String id) {
        return genericResourceService.get(
                getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
    }

    public NewProviderBundle get(String id, String catalogueId) {
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        //FIXME: never reaches here
        if (catalogueBundle == null) {
            throw new CatalogueResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", catalogueId));
        }
        return genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("catalogue_id", catalogueId),
                new SearchService.KeyValue("published", "false")
        );
    }

    public NewProviderBundle get(SearchService.KeyValue... keyValues) {
        return genericResourceService.get(getResourceTypeName(), keyValues);
    }

    public Browsing<R> getAll(FacetFilter ff, Authentication auth) {
        ff.setResourceType(getResourceTypeName());
        boolean authenticated = auth != null && auth.isAuthenticated();
        if (authenticated) {
            if (securityService.hasPortalAdminRole(auth)) {
                return getAll(ff);
            }
            if (securityService.hasRole(auth, "ROLE_PROVIDER")) {
                ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
                return getAll(ff);
            }
        }
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        return getAll(ff);
    }

    public Browsing<R> getAll(FacetFilter ff) {
        ff.setResourceType(getResourceTypeName());
        return genericResourceService.getResults(ff);
    }
}
