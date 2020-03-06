package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("pendingProviderManager")
public class PendingProviderManager extends ResourceManager<ProviderBundle> implements PendingResourceService<ProviderBundle> {

    private static final Logger logger = LogManager.getLogger(PendingProviderManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerManager;
    private final PendingResourceService<InfraService> pendingServiceManager;
    private final InfraServiceManager infraServiceManager;
    private final IdCreator idCreator;
    private final SecurityService securityService;

    @Autowired
    public PendingProviderManager(ProviderService<ProviderBundle, Authentication> providerManager,
                                  @Lazy PendingResourceService<InfraService> pendingServiceManager,
                                  InfraServiceManager infraServiceManager, IdCreator idCreator,
                                  @Lazy SecurityService securityService) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.pendingServiceManager = pendingServiceManager;
        this.infraServiceManager = infraServiceManager;
        this.idCreator = idCreator;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "pending_provider";
    }

    @Override
    public ProviderBundle add(ProviderBundle providerBundle, Authentication auth) {

        providerBundle.setId(idCreator.createProviderId(providerBundle.getProvider()));
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), new User(auth).getFullName()));

        if (providerBundle.getStatus() == null) {
            providerBundle.setStatus(Provider.States.PENDING_1.getKey());
        }

        super.add(providerBundle, auth);

        return providerBundle;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication auth) {
        String newId = StringUtils
                .stripAccents(providerBundle.getProvider().getAcronym())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_");
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), new User(auth).getFullName()));
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerBundle.getId());
        ff.setQuantity(10000);

        // if provider id has changed
        if (!providerBundle.getId().equals(newId)) {
            // update PendingServices of this provider
            List<InfraService> providerPendingServices = pendingServiceManager.getAll(ff, auth).getResults();
            for (InfraService service : providerPendingServices) {
                updateProviderId(service, providerBundle.getId(), newId);
                pendingServiceManager.update(service, auth);
            }

            // update InfraServices of this provider
            List<InfraService> providerServices = infraServiceManager.getAll(ff, auth).getResults();
            for (InfraService service : providerServices) {
                updateProviderId(service, providerBundle.getId(), newId);
                infraServiceManager.update(service, auth);
            }
        }

        // get existing resource
        Resource existing = whereID(providerBundle.getId(), true);
        // change provider id
        providerBundle.getProvider().setId(newId);
        // save existing resource with new payload
        existing.setPayload(serialize(providerBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating PendingProvider: {}", providerBundle);
        return providerBundle;
    }

    @Override
    public ProviderBundle transformToPending(ProviderBundle providerBundle, Authentication auth) {
        return transformToPending(providerBundle.getId(), auth);
    }

    @Override
    public ProviderBundle transformToPending(String providerId, Authentication auth) {
        Resource resource = providerManager.getResource(providerId);
        resource.setResourceTypeName("provider"); //make sure that resource type is present
        resourceService.changeResourceType(resource, resourceType);
        return deserialize(resource);
    }

    @Override
    public ProviderBundle transformToActive(ProviderBundle providerBundle, Authentication auth) {
        providerManager.validate(providerBundle);
        providerBundle = update(providerBundle, auth);
        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getResource(providerBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);
        return providerBundle;
    }

    @Override
    public ProviderBundle transformToActive(String providerId, Authentication auth) {
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

    private InfraService updateProviderId(InfraService service, String oldId, String newId) {
        List<String> providerIds = service.getService().getProviders();
        providerIds = providerIds.stream().map(id -> {
            if (id.equals(oldId)) {
                return newId;
            } else {
                return id;
            }
        }).collect(Collectors.toList());
        service.getService().setProviders(providerIds);
        return service;
    }

    public boolean userIsPendingProviderAdmin(Authentication auth, ProviderBundle registeredProvider) {
        User user = new User(auth);
        if (registeredProvider == null) {
            throw new ResourceNotFoundException("Provider with id '" + registeredProvider.getId() + "' does not exist.");
        }
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
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return super.getAll(ff, auth).getResults()
                .stream().map(p -> {
                    if (userIsPendingProviderAdmin(auth, p)) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
