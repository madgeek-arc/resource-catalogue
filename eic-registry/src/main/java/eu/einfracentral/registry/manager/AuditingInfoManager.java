package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.AuditingInfoService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AuditingInfoManager implements AuditingInfoService<BundleStatus, Authentication> {

    private final ProviderService providerService;
    private final InfraServiceService infraServiceService;

    @Autowired
    public AuditingInfoManager(ProviderService providerService, InfraServiceService infraServiceService) {
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
    }


    public List<ProviderBundle> getRandomProviders(Authentication auth){
        List<ProviderBundle> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("actionType", BundleStatus.ActionTypes.INVALID);
        List<ProviderBundle> allProviders = providerService.getAll(ff, auth).getResults();
        Collections.shuffle(allProviders);
        for (int i=0; i<10; i++){
            ret.add(allProviders.get(i));
        }
        return ret;
    }

    public List<InfraService> getRandomResources(Authentication auth){
        List<InfraService> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("actionType", BundleStatus.ActionTypes.INVALID);
        List<InfraService> allServices = infraServiceService.getAll(ff, auth).getResults();
        Collections.shuffle(allServices);
        for (int i=0; i<10; i++){
            ret.add(allServices.get(i));
        }
        return ret;
    }

    public ProviderBundle auditProvider (String providerId, String actionType, String comment, Authentication auth) throws ResourceNotFoundException {
        BundleStatus bundleStatus = new BundleStatus();
        ProviderBundle provider = (ProviderBundle) providerService.get(providerId);
        bundleStatus.setUser(User.of(auth).getEmail());
        bundleStatus.setComment(comment);
        bundleStatus.setActionType(actionType);
        bundleStatus.setDate(String.valueOf(System.currentTimeMillis()));
        provider.setAuditStatus(bundleStatus);
        providerService.update(provider, "audit", auth);
        return provider;
    }

}
