package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PidManager implements PIDService {

    private final PidIssuer pidIssuer;
    private final GenericResourceService genericResourceService;
    private final CatalogueProperties catalogueProperties;

    public PidManager(PidIssuer pidIssuer,
                      GenericResourceService genericResourceService,
                      CatalogueProperties catalogueProperties) {
        this.pidIssuer = pidIssuer;
        this.genericResourceService = genericResourceService;
        this.catalogueProperties = catalogueProperties;
    }

    @Override
    public Bundle<?> get(String prefix, String suffix) {
        String pid = prefix + "/" + suffix;
        String resourceType = catalogueProperties.getResourceTypeFromPrefix(prefix);
        if (resourceType != null) {
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(10000);
            ff.setResourceType(resourceType);
            ff.addFilter("resource_internal_id", pid);
            Browsing<Bundle<?>> browsing = genericResourceService.getResults(ff);
            if (!browsing.getResults().isEmpty()) {
                return browsing.getResults().getFirst();
            }
        }
        return null;
    }

    public void register(String pid, List<String> endpoints) {
        pidIssuer.postPID(pid, endpoints);
    }
}
