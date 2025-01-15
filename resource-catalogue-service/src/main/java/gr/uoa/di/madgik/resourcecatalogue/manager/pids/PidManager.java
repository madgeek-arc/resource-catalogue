package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import org.springframework.stereotype.Service;


@Service
public class PidManager implements PIDService {

    private final PidIssuer pidIssuer;
    private final GenericResourceService genericResourceService;

    public PidManager(PidIssuer pidIssuer,
                      GenericResourceService genericResourceService) {
        this.pidIssuer = pidIssuer;
        this.genericResourceService = genericResourceService;
    }

    @Override
    public Bundle<?> get(String prefix, String suffix) {
        String pid = prefix + "/" + suffix;
        String resourceType = pidIssuer.determineResourceTypeFromPidPrefix(prefix);
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

    @Override
    public void register(String pid) {
        pidIssuer.postPID(pid);
    }
}
