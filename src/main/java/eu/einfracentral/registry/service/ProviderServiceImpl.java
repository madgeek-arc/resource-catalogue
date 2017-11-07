package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;

/**
 * Created by pgl on 26/7/2017.
 */
@org.springframework.stereotype.Service("providerService")
public class ProviderServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<Provider> implements ProviderService {
    public ProviderServiceImpl() {
        super(Provider.class);
    }

    @Override
    public String getResourceType() {
        return "provider";
    }
}
