package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import gr.uoa.di.madgik.resourcecatalogue.utils.PIDUtils;

@org.springframework.stereotype.Service("pidManager")
public class PIDManager implements PIDService {

    private final PIDUtils pidUtils;
    private final GenericResourceService genericResourceService;

    public PIDManager(PIDUtils pidUtils,
                      GenericResourceService genericResourceService) {
        this.pidUtils = pidUtils;
        this.genericResourceService = genericResourceService;
    }

    public Bundle<?> get(String prefix, String suffix) {
        String pid = prefix + "/" + suffix;
        String resourceType = pidUtils.determineResourceTypeFromPidPrefix(prefix);
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
        pidUtils.postPID(pid);
    }
}
