package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@Service("pendingProviderManager")
public class PendingProviderManager extends ResourceManager<ProviderBundle> implements PendingResourceService<ProviderBundle> {

    private static final Logger logger = LogManager.getLogger(PendingProviderManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerManager;
    private final IdCreator idCreator;

    @Autowired
    public PendingProviderManager(ProviderService<ProviderBundle, Authentication> providerManager,
                                  IdCreator idCreator) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
    }


    @Override
    public String getResourceType() {
        return "pending_provider";
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle providerBundle, Authentication auth) {

        providerBundle.setId(idCreator.createProviderId(providerBundle.getProvider()));
        logger.trace("User '{}' is attempting to add a new Pending Provider: {}", auth, providerBundle);
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName()));

        if (providerBundle.getStatus() == null) {
            providerBundle.setStatus(Provider.States.PENDING_1.getKey());
        }

        super.add(providerBundle, auth);

        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle providerBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Pending Provider: {}", auth, providerBundle);
        // get existing resource
        Resource existing = whereID(providerBundle.getId(), true);
        // save existing resource with new payload
        existing.setPayload(serialize(providerBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating PendingProvider: {}", providerBundle);
        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToPending(ProviderBundle providerBundle, Authentication auth) {
        return transformToPending(providerBundle.getId(), auth);
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToPending(String providerId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Active Provider with id '{}' to Pending", auth, providerId);
        Resource resource = providerManager.getResource(providerId);
        resource.setResourceTypeName("provider"); //make sure that resource type is present
        resourceService.changeResourceType(resource, resourceType);
        return deserialize(resource);
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToActive(ProviderBundle providerBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Provider with id '{}' to Active", auth, providerBundle.getId());
        providerManager.validate(providerBundle);
        providerBundle = update(providerBundle, auth);
        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getResource(providerBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);
        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToActive(String providerId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Provider with id {} to Active", auth, providerId);
        ProviderBundle providerBundle = get(providerId);
        providerManager.validate(providerBundle);
        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getResource(providerId);
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);
        return providerBundle;
    }


    @Override
    public Object getPendingRich(String id, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }


    public boolean userIsPendingProviderAdmin(@NotNull User user, @NotNull ProviderBundle registeredProvider) {
        if (registeredProvider.getProvider().getUsers() == null) {
            return false;
        }
        return registeredProvider.getProvider().getUsers()
                .parallelStream()
                .filter(Objects::nonNull)
                .anyMatch(u -> {
                    if (u.getId() != null) {
                        if (u.getEmail() != null) {
                            return u.getId().equals(user.getId())
                                    || u.getEmail().equals(user.getEmail());
                        }
                        return u.getId().equals(user.getId());
                    }
                    return u.getEmail().equals(user.getEmail());
                });
    }


    public List<ProviderBundle> getMy(Authentication auth) {
        if (auth == null) {
            return new ArrayList<>();
        }
        User user = User.of(auth);
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return super.getAll(ff, auth).getResults()
                .stream().map(p -> {
                    if (userIsPendingProviderAdmin(user, p)) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
