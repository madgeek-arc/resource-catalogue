package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.ProviderService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 26/7/2017.
 */
@Service("providerService")
public class ProviderServiceImpl extends ResourceServiceImpl<Provider> implements ProviderService {
    public ProviderServiceImpl() {
        super(Provider.class);
    }

    @Override
    public String getResourceType() {
        return "provider";
    }
}
