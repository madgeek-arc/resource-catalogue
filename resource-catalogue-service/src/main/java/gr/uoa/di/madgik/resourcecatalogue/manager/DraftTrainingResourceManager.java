package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.*;

@Service("draftTrainingResourceManager")
public class DraftTrainingResourceManager extends ResourceManager<TrainingResourceBundle> implements DraftResourceService<TrainingResourceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftServiceManager.class);

    private final TrainingResourceService trainingResourceService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftTrainingResourceManager(TrainingResourceService trainingResourceService,
                                        IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                                        @Lazy ProviderService providerService,
                                        ProviderResourcesCommonMethods commonMethods) {
        super(TrainingResourceBundle.class);
        this.trainingResourceService = trainingResourceService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "draft_training_resource";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication auth) {

        trainingResourceBundle.setId(idCreator.generate(getResourceType()));

        // Check if there is a Resource with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<TrainingResourceBundle> resourceList = trainingResourceService.getAll(ff, auth).getResults();
        for (TrainingResourceBundle existingTR : resourceList) {
            if (trainingResourceBundle.getTrainingResource().getId().equals(existingTR.getTrainingResource().getId()) && existingTR.getTrainingResource().getCatalogueId().equals(catalogueId)) {
                throw new ResourceAlreadyExistsException(String.format("Training Resource with the specific id already exists on the [%s] Catalogue", catalogueId));
            }
        }
        logger.trace("Attempting to add a new Draft Training Resource with id {}", trainingResourceBundle.getId());

        if (trainingResourceBundle.getMetadata() == null) {
            trainingResourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        trainingResourceBundle.getTrainingResource().setCatalogueId(catalogueId);
        trainingResourceBundle.setActive(false);

        super.add(trainingResourceBundle, auth);

        return trainingResourceBundle;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ResponseEntity<TrainingResourceBundle> addCrud(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        trainingResourceBundle.setId(idCreator.generate(getResourceType()));
        super.add(trainingResourceBundle, auth);
        logger.debug("Created a new Draft Training Resource with id {}", trainingResourceBundle.getId());
        return new ResponseEntity<>(trainingResourceBundle, HttpStatus.CREATED);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        // get existing resource
        Resource existing = getPendingResourceViaTrainingResourceId(trainingResourceBundle.getTrainingResource().getId());
        // block catalogueId updates from Provider Admins
        trainingResourceBundle.getTrainingResource().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Training Resource with id {}", trainingResourceBundle.getId());
        trainingResourceBundle.setMetadata(Metadata.updateMetadata(trainingResourceBundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(trainingResourceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Training Resource: {}", trainingResourceBundle);
        return trainingResourceBundle;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        super.delete(trainingResourceBundle);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle transformToNonDraft(String id, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = this.get(id);
        return transformToNonDraft(trainingResourceBundle, auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle transformToNonDraft(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Training Resource with id {} to Active", trainingResourceBundle.getId());
        trainingResourceService.validate(trainingResourceBundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(trainingResourceBundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation()).getTemplateStatus().equals("approved template")) {
            trainingResourceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);
            trainingResourceBundle.setActive(true);
        } else {
            trainingResourceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }
        trainingResourceBundle.setLoggingInfo(loggingInfoList);
        // latestOnboardingInfo
        trainingResourceBundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        trainingResourceBundle.setMetadata(Metadata.updateMetadata(trainingResourceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType infraResourceType = resourceTypeService.getResourceType("training_resource");
        Resource resource = getPendingResourceViaTrainingResourceId(trainingResourceBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            trainingResourceBundle = trainingResourceService.update(trainingResourceBundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return trainingResourceBundle;
    }

    public List<TrainingResourceBundle> getMy(Authentication auth) {
        //TODO: Implement
        List<TrainingResourceBundle> re = new ArrayList<>();
        return re;
    }

    private Resource getPendingResourceViaTrainingResourceId(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

}
