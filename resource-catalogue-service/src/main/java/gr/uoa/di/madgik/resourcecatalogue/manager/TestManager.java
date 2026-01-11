package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.TestService;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//TODO: resource-specific method -> inside corresponding manager/service
//TODO: true universal method (all resources will use it) -> inside here (generic)
//TODO: some but not all resources need a method -> create a new interface with default implementation

@org.springframework.stereotype.Service("testManager")
public abstract class TestManager<T extends NewBundle> implements TestService<T> {

    private final GenericResourceService genericResourceService;
    private final SecurityService securityService;
    private final CatalogueService catalogueService;

    private static final Logger logger = LoggerFactory.getLogger(TestManager.class);

    protected abstract String getResourceTypeName();

    public TestManager(GenericResourceService genericResourceService,
                       SecurityService securityService,
                       CatalogueService catalogueService) {
        this.genericResourceService = genericResourceService;
        this.securityService = securityService;
        this.catalogueService = catalogueService;
    }

    //TODO: we don't need this
    @Override
    public T get(String id) {
        return genericResourceService.get(
                getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
    }

    @Override
    public T get(String id, String catalogueId) {
        if (catalogueId != null && !catalogueId.isBlank()) {
            return genericResourceService.get(getResourceTypeName(),
                    new SearchService.KeyValue("resource_internal_id", id),
                    new SearchService.KeyValue("catalogue_id", catalogueId),
                    new SearchService.KeyValue("published", "false"));
        }
        return genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"));
    }

    //TODO: probably we do not need this IF we use the same get for drafts and non-drafts
    //TODO: draft functionality is default-catalogue specific, meaning the IDs are always unique
    @Override
    public T get(SearchService.KeyValue... keyValues) {
        return genericResourceService.get(getResourceTypeName(), keyValues);
    }

    @Override
    public Browsing<T> getAll(FacetFilter ff, Authentication auth) {
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

    @Override
    public Browsing<T> getAll(FacetFilter ff) {
        ff.setResourceType(getResourceTypeName());
        return genericResourceService.getResults(ff);
    }

    @Override
    public Browsing<T> getMy(FacetFilter ff, Authentication auth) {
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    @Override
    public T add(T bundle, Authentication auth) {
        return genericResourceService.add(getResourceTypeName(), bundle);
    }

    @Override
    public T update(T bundle, Authentication auth) {
        if (!hasChanged(bundle)) {
            return bundle;
        }
        bundle.markUpdate(auth, null); //TODO: make sure all resources will use this
        validate(bundle);
        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasChanged(T bundle) {
        T existing = get(bundle.getId(), bundle.getCatalogueId());
        return !bundle.equals(existing);
    }

    @Override
    public T audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        T existing = get(id, catalogueId);
        existing.markAudit(comment, actionType, auth);

        // send notification emails to Provider Admins
//        mailService.notifyProviderAdminsForBundleAuditing(existing, existing.getProvider().get("users")); //FIXME

        logger.info("Audited '{}' with ID '{}' [actionType: {}]", getResourceTypeName(), existing.getId(), actionType);

        try {
            genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return existing;
    }

    @Override
    public Paging<T> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000);
        ff.addFilter("status", "approved");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);

        Browsing<T> resourcesBrowsing = getAll(ff, auth);
        List<T> resourcesToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(auditingInterval)
                .toEpochSecond();

        for (T bundle : resourcesBrowsing.getResults()) {
            LoggingInfo auditInfo = bundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include providers that have never been audited
                resourcesToBeAudited.add(bundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include providers that were last audited before the threshold
                        resourcesToBeAudited.add(bundle);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(resourcesToBeAudited);

        // Limit the list to the requested quantity
        if (resourcesToBeAudited.size() > quantity) {
            resourcesToBeAudited = resourcesToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(resourcesToBeAudited.size(), 0, resourcesToBeAudited.size(), resourcesToBeAudited,
                resourcesBrowsing.getFacets());
    }

    //region unused
    @Override
    public String createId(T bundle) {
        return "";
    }

    @Override
    public T save(T bundle) {
        return null;
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        return Map.of();
    }

    @Override
    public List<T> getSome(String... ids) {
        return List.of();
    }

    @Override
    public List<T> delAll() {
        return List.of();
    }

    @Override
    public T validate(T bundle) {
        logger.debug("Validating resource '{}' with id: '{}'", getResourceTypeName(), bundle.getId());
        return genericResourceService.validate(getResourceTypeName(), bundle);
    }

    @Override
    public Resource getResource(String id) {
        return null;
    }

    @Override
    public Resource getResource(String id, String catalogueId) {
        return null;
    }

    @Override
    public boolean exists(T bundle) {
        return false;
    }

    @Override
    public boolean exists(String id) {
        return false;
    }

    //TODO: move to PublicController
    @Override
    public T createPublicResource(T bundle, Authentication auth) {
        return null;
    }
    //endregion
}
