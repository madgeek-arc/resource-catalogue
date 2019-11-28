package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("pendingProviderManager")
public class PendingProviderManager extends ResourceManager<ProviderBundle> implements ResourceService<ProviderBundle, Authentication> {

    @Autowired
    public PendingProviderManager() {
        super(ProviderBundle.class);
    }

    @Override
    public String getResourceType() {
        return "pending_provider";
    }

    @Override
    public ProviderBundle add(ProviderBundle provider, Authentication auth) {

        provider.setId(Provider.createId(provider.getProvider()));

        if (provider.getStatus() == null){
            provider.setStatus(Provider.States.PENDING_1.getKey());
        }

        super.add(provider, auth);

        return provider;
    }

}
