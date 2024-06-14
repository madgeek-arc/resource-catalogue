package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_PROVIDERS;

@Service("draftProviderManager")
public class DraftProviderManager extends ResourceManager<ProviderBundle> implements DraftResourceService<ProviderBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftProviderManager.class);

    private final ProviderService providerManager;
    private final IdCreator idCreator;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftProviderManager(ProviderService providerManager,
                                IdCreator idCreator, @Lazy RegistrationMailService registrationMailService,
                                @Lazy VocabularyService vocabularyService,
                                ProviderResourcesCommonMethods commonMethods) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
    }


    @Override
    public String getResourceType() {
        return "draft_provider";
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public ProviderBundle get(String id) {
        ProviderBundle provider = super.get(id);
        if (provider == null) {
            throw new gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException(
                    String.format("Could not find draft provider with id: %s", id));
        }
        return provider;
    }


    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle providerBundle, Authentication auth) {

        providerBundle.setId(idCreator.generate(getResourceType()));

        // Check if there is a Provider with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providerList = providerManager.getAll(ff, auth).getResults();
        for (ProviderBundle existingProvider : providerList) {
            if (providerBundle.getProvider().getId().equals(existingProvider.getProvider().getId()) && existingProvider.getProvider().getCatalogueId().equals(catalogueId)) {
                throw new ResourceAlreadyExistsException(String.format("Provider with the specific id already exists on the [%s] Catalogue.", catalogueId));
            }
        }
        logger.trace("Attempting to add a new Draft Provider: {}", providerBundle);
        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        providerBundle.setLoggingInfo(loggingInfoList);

        providerBundle.getProvider().setCatalogueId(catalogueId);

        super.add(providerBundle, auth);

        return providerBundle;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle providerBundle, Authentication auth) {
        // get existing resource
        Resource existing = getPendingResourceViaProviderId(providerBundle.getId());
        // block catalogueId updates from Provider Admins
        providerBundle.getProvider().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Provider: {}", providerBundle);
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
    public ProviderBundle transformToNonDraft(String providerId, Authentication auth) {
        ProviderBundle providerBundle = get(providerId);
        return transformToNonDraft(providerBundle, auth);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle transformToNonDraft(ProviderBundle providerBundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Provider with id '{}' to Active", providerBundle.getId());
        providerManager.validate(providerBundle);
        if (providerManager.exists(providerBundle)) {
            throw new ResourceAlreadyExistsException(String.format("Provider with id = '%s' already exists!", providerBundle.getId()));
        }

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(providerBundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        providerBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardInfo
        providerBundle.setLatestOnboardingInfo(loggingInfo);

        // update providerStatus
        providerBundle.setStatus(vocabularyService.get("pending provider").getId());
        providerBundle.setTemplateStatus(vocabularyService.get("no template status").getId());

        providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getPendingResourceViaProviderId(providerBundle.getId());
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

    public List<ProviderBundle> getMy(Authentication auth) {
        if (auth == null) {
            return new ArrayList<>();
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("users", User.of(auth).getEmail());
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth).getResults();
    }

    private Resource getPendingResourceViaProviderId(String providerId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", providerId, catalogueId),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }
}
