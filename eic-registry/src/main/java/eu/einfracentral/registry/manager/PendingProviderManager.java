package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("pendingProviderManager")
public class PendingProviderManager extends ResourceManager<ProviderBundle> implements PendingResourceService<ProviderBundle> {

    private static final Logger logger = LogManager.getLogger(PendingProviderManager.class);
    private final ProviderService<ProviderBundle, Authentication> providerManager;

    @Autowired
    public PendingProviderManager(ProviderService<ProviderBundle, Authentication> providerManager) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
    }

    @Override
    public String getResourceType() {
        return "pending_provider";
    }

    @Override
    public ProviderBundle add(ProviderBundle provider, Authentication auth) {

        provider.setId(Provider.createId(provider.getProvider()));

        if (provider.getStatus() == null) {
            provider.setStatus(Provider.States.PENDING_1.getKey());
        }

        super.add(provider, auth);

        return provider;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication auth) {
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), new User(auth).getFullName()));
        Resource existing = whereID(providerBundle.getId(), true);
        providerBundle.getProvider().setId(eu.einfracentral.domain.Provider.createId(providerBundle.getProvider()));
        existing.setPayload(serialize(providerBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Pending Provider: {}", providerBundle);
        return providerBundle;
    }

    @Override
    public void transformToPending(String providerId) {
        Resource resource = providerManager.getResource(providerId);
        resource.setResourceTypeName("provider"); //make sure that resource type is present
        resourceService.changeResourceType(resource, resourceType);
    }

    @Override
    public void transformToActive(String providerId) {
        providerManager.validate(get(providerId));
        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getResource(providerId);
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);
    }

    @Override
    public Object getPendingRich(String id, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }
}
