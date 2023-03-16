package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public PendingProviderManager(ProviderService<ProviderBundle, Authentication> providerManager,
                                  IdCreator idCreator, @Lazy RegistrationMailService registrationMailService,
                                  @Lazy SecurityService securityService, @Lazy VocabularyService vocabularyService) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
        this.registrationMailService = registrationMailService;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
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
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providerList = providerManager.getAll(ff, auth).getResults();
        for (ProviderBundle existingProvider : providerList){
            if (providerBundle.getProvider().getId().equals(existingProvider.getProvider().getId()) && existingProvider.getProvider().getCatalogueId().equals(catalogueName)) {
                throw new ValidationException(String.format("Provider with the specific id already exists on the [%s] Catalogue. Please refactor your 'abbreviation' field.", catalogueName));
            }
        }
        logger.trace("User '{}' is attempting to add a new Pending Provider: {}", auth, providerBundle);
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.DRAFT.getKey(), LoggingInfo.ActionType.CREATED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        providerBundle.setLoggingInfo(loggingInfoList);

        providerBundle.getProvider().setCatalogueId(catalogueName);

        super.add(providerBundle, auth);

        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle providerBundle, Authentication auth) {
        // get existing resource
        Resource existing = whereID(providerBundle.getId(), true);
        // block catalogueId updates from Provider Admins
        providerBundle.getProvider().setCatalogueId(catalogueName);
        logger.trace("User '{}' is attempting to update the Pending Provider: {}", auth, providerBundle);
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
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
        Resource resource = providerManager.getResource(providerId, catalogueName);
        resource.setResourceTypeName("provider");
        resourceService.changeResourceType(resource, resourceType);
        return deserialize(resource);
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToActive(ProviderBundle providerBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Provider with id '{}' to Active", auth, providerBundle.getId());
        providerManager.validate(providerBundle);
        if (providerManager.exists(providerBundle)) {
            throw new ResourceException(String.format("Provider with id = '%s' already exists!", providerBundle.getId()), HttpStatus.CONFLICT);
        }

        // update loggingInfo
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList  = new ArrayList<>();
        if (providerBundle.getLoggingInfo() != null) {
            loggingInfoList = providerBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        providerBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardInfo
        providerBundle.setLatestOnboardingInfo(loggingInfo);

        // update providerStatus
        providerBundle.setStatus(vocabularyService.get("pending provider").getId());
        providerBundle.setTemplateStatus(vocabularyService.get("no template status").getId());

        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = this.getPendingResourceViaProviderId(providerBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);

        try {
            providerBundle = providerManager.update(providerBundle, auth);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }

        registrationMailService.sendEmailsToNewlyAddedAdmins(providerBundle, null);
        return providerBundle;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToActive(String providerId, Authentication auth) {
        ProviderBundle providerBundle = get(providerId);
        return transformToActive(providerBundle, auth);
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
                                    || u.getEmail().equalsIgnoreCase(user.getEmail());
                        }
                        return u.getId().equals(user.getId());
                    }
                    return u.getEmail().equalsIgnoreCase(user.getEmail());
                });
    }


    public List<ProviderBundle> getMy(Authentication auth) {
        if (auth == null) {
            return new ArrayList<>();
        }
        User user = User.of(auth);
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("users", User.of(auth).getEmail());
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth).getResults();
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

    public Resource getPendingResourceViaProviderId(String providerId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("pending_provider_id = \"%s\" AND catalogue_id = \"%s\"", providerId, catalogueName), resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    public Resource getPendingResourceViaServiceId(String serviceId){
        return null;
    }
    public DatasourceBundle getOpenAIREDatasource(Datasource datasource){
        return null;
    }
}
