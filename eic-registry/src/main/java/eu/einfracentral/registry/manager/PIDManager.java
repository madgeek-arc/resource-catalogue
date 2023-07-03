package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.registry.service.PIDService;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;

@org.springframework.stereotype.Service("pidManager")
public class PIDManager implements PIDService {

    private final ProviderResourcesCommonMethods commonMethods;

    public PIDManager(ProviderResourcesCommonMethods commonMethods) {
        this.commonMethods = commonMethods;
    }

    public Bundle<?> get(String resourceType, String pid) {
        return commonMethods.getPublicResourceViaPID(resourceType, pid);
    }

}
