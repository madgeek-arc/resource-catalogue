package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;

@org.springframework.stereotype.Service("pidManager")
public class PIDManager implements PIDService {

    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;

    public PIDManager(ProviderResourcesCommonMethods commonMethods,
                      GenericResourceService genericResourceService) {
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
    }

    public Bundle<?> get(String prefix, String suffix) {
        String pid = prefix + "/" + suffix;
        String resourceType = commonMethods.determineResourceTypeFromPidPrefix(prefix);
        if (!resourceType.equals("no_resource_type")) {
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(10000);
            ff.setResourceType(resourceType);
            ff.addFilter("resource_internal_id", pid);
            Browsing<Bundle<?>> browsing = genericResourceService.getResults(ff);
            if (!browsing.getResults().isEmpty()) {
                return browsing.getResults().get(0);
            }
        }
        return null;
    }

    public void register(String pid) {
        commonMethods.postPID(pid);
    }
}
