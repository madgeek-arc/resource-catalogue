package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.ProviderService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 26/7/2017.
 */
@Service("providerService")
public class ProviderManager extends ResourceManager<Provider> implements ProviderService {
    public ProviderManager() {
        super(Provider.class);
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public Integer visits(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
