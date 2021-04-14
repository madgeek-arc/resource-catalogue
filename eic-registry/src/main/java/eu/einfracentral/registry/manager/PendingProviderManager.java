package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
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
    private final RegistrationMailService registrationMailService;

    @Autowired
    public PendingProviderManager(ProviderService<ProviderBundle, Authentication> providerManager,
                                  IdCreator idCreator, @Lazy RegistrationMailService registrationMailService) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
        this.registrationMailService = registrationMailService;
    }


    @Override
    public String getResourceType() {
        return "pending_provider";
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public ProviderBundle get(String id) {
        ProviderBundle provider = super.get(id);
        if (provider == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find pending provider with id: %s", id));
        }
        return provider;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle providerBundle, Authentication auth) {

        providerBundle.setId(idCreator.createProviderId(providerBundle.getProvider()));

        // Check if there is a Provider with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<ProviderBundle> providerList = providerManager.getAll(ff, auth).getResults();
        for (ProviderBundle existingProvider : providerList){
            if (providerBundle.getProvider().getId().equals(existingProvider.getProvider().getId())) {
                throw new ValidationException("Provider with the specific id already exists. Please refactor your 'abbreviation' field.");
            }
        }
        logger.trace("User '{}' is attempting to add a new Pending Provider: {}", auth, providerBundle);
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        if (providerBundle.getStatus() == null) {
            providerBundle.setStatus("pending initial approval");
        }

        super.add(providerBundle, auth);

        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle providerBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Pending Provider: {}", auth, providerBundle);
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
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
    public void delete(ProviderBundle providerBundle) {
        super.delete(providerBundle);
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
        if (providerBundle.getProvider().getScientificDomains() != null && !providerBundle.getProvider().getScientificDomains().isEmpty()) {
            providerManager.validateScientificDomains(providerBundle.getProvider().getScientificDomains());
        }
        if (providerBundle.getProvider().getMerilScientificDomains() != null && !providerBundle.getProvider().getMerilScientificDomains().isEmpty()){
            providerManager.validateMerilScientificDomains(providerBundle.getProvider().getMerilScientificDomains());
        }
        if (providerManager.exists(providerBundle)) {
            throw new ResourceException(String.format("%s with id = '%s' already exists!", resourceType.getName(), providerBundle.getId()), HttpStatus.CONFLICT);
        }
        providerBundle = update(providerBundle, auth);
        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getResource(providerBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);

        registrationMailService.sendEmailsToNewlyAddedAdmins(providerBundle, null);
        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToActive(String providerId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Provider with id {} to Active", auth, providerId);
        ProviderBundle providerBundle = get(providerId);

        if (providerManager.exists(providerBundle)) {
            throw new ResourceException(String.format("%s with id = '%s' already exists!", resourceType.getName(), providerBundle.getId()), HttpStatus.CONFLICT);
        }
        providerManager.validate(providerBundle);
        if (providerBundle.getProvider().getScientificDomains() != null && !providerBundle.getProvider().getScientificDomains().isEmpty()) {
            providerManager.validateScientificDomains(providerBundle.getProvider().getScientificDomains());
        }
        if (providerBundle.getProvider().getMerilScientificDomains() != null && !providerBundle.getProvider().getMerilScientificDomains().isEmpty()){
            providerManager.validateMerilScientificDomains(providerBundle.getProvider().getMerilScientificDomains());
        }
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getResource(providerId);
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);

        registrationMailService.sendEmailsToNewlyAddedAdmins(providerBundle, null);
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

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth){
        ProviderBundle providerBundle = get(providerId);
        List<String> userList = new ArrayList<>();
        for (User user : providerBundle.getProvider().getUsers()){
            userList.add(user.getEmail());
        }
        if ((providerBundle.getMetadata().getTerms() == null || providerBundle.getMetadata().getTerms().isEmpty())) {
            if (userList.contains(User.of(auth).getEmail())) {
                return false; //pop-up modal
            } else {
                return true; //no modal
            }
        }
        if (!providerBundle.getMetadata().getTerms().contains(User.of(auth).getEmail()) && userList.contains(User.of(auth).getEmail())) {
            return false; // pop-up modal
        }
        return true; // no modal
    }

    public void adminAcceptedTerms(String providerId, Authentication auth){
        update(get(providerId), auth);
    }

}
