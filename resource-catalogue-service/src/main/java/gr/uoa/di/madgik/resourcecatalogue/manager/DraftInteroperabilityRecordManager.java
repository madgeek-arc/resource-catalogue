package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.*;

@Deprecated(forRemoval = true)
@Service("draftInteroperabilityRecordManager")
public class DraftInteroperabilityRecordManager extends ResourceManager<InteroperabilityRecordBundle> implements DraftResourceService<InteroperabilityRecordBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftInteroperabilityRecordManager.class);

    private final InteroperabilityRecordService interoperabilityRecordService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftInteroperabilityRecordManager(InteroperabilityRecordService interoperabilityRecordService,
                                              IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                                              @Lazy ProviderService providerService,
                                              ProviderResourcesCommonMethods commonMethods) {
        super(InteroperabilityRecordBundle.class);
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "draft_interoperability_record";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {

        interoperabilityRecordBundle.setId(idCreator.generate(getResourceType()));

        // Check if there is a Resource with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<InteroperabilityRecordBundle> resourceList = interoperabilityRecordService.getAll(ff, auth).getResults();
        for (InteroperabilityRecordBundle existingIR : resourceList) {
            if (interoperabilityRecordBundle.getInteroperabilityRecord().getId().equals(existingIR.getInteroperabilityRecord().getId()) && existingIR.getInteroperabilityRecord().getCatalogueId().equals(catalogueId)) {
                throw new ResourceAlreadyExistsException(String.format("Interoperability Record with the specific id already exists on the [%s] Catalogue", catalogueId));
            }
        }
        logger.trace("Attempting to add a new Draft Interoperability Record with id {}", interoperabilityRecordBundle.getId());

        if (interoperabilityRecordBundle.getMetadata() == null) {
            interoperabilityRecordBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(catalogueId);
        interoperabilityRecordBundle.setActive(false);

        super.add(interoperabilityRecordBundle, auth);

        return interoperabilityRecordBundle;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ResponseEntity<InteroperabilityRecordBundle> addCrud(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        interoperabilityRecordBundle.setId(idCreator.generate(getResourceType()));
        super.add(interoperabilityRecordBundle, auth);
        logger.debug("Created a new Draft Interoperability Record with id {}", interoperabilityRecordBundle.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.CREATED);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        // get existing resource
        Resource existing = getPendingResourceViaInteroperabilityRecordId(interoperabilityRecordBundle.getInteroperabilityRecord().getId());
        // block catalogueId updates from Provider Admins
        interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Interoperability Record with id {}", interoperabilityRecordBundle.getId());
        interoperabilityRecordBundle.setMetadata(Metadata.updateMetadata(interoperabilityRecordBundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(interoperabilityRecordBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Interoperability Record: {}", interoperabilityRecordBundle);
        return interoperabilityRecordBundle;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        super.delete(interoperabilityRecordBundle);
    }

    @Override
    public InteroperabilityRecordBundle transformToDraft(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        return null;
    }

    @Override
    public InteroperabilityRecordBundle transformToDraft(String id, Authentication auth) {
        return null;
    }

    @Override
    public String getDraftResourceType() {
        return null;
    }

    @Override
    public InteroperabilityRecordBundle addDraft(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        return null;
    }

    @Override
    public InteroperabilityRecordBundle updateDraft(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        return null;
    }

    @Override
    public void deleteDraft(String id, Authentication authentication) {

    }

    @Override
    public InteroperabilityRecordBundle getDraft(String id, Authentication authentication) {
        return null;
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getAllDrafts(FacetFilter facetFilter, Authentication authentication) {
        return null;
    }

    @Override
    public List<InteroperabilityRecordBundle> getMyDrafts(Authentication authentication) {
        return null;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle transformToNonDraft(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Interoperability Record with id {} to Active", interoperabilityRecordBundle.getId());
        interoperabilityRecordService.validate(interoperabilityRecordBundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(interoperabilityRecordBundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        interoperabilityRecordBundle.setStatus("pending interoperability record");
        // latestOnboardingInfo
        interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        interoperabilityRecordBundle.setMetadata(Metadata.updateMetadata(interoperabilityRecordBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType infraResourceType = resourceTypeService.getResourceType("interoperability_record");
        Resource resource = getPendingResourceViaInteroperabilityRecordId(interoperabilityRecordBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            interoperabilityRecordBundle = interoperabilityRecordService.update(interoperabilityRecordBundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return interoperabilityRecordBundle;
    }

    public List<InteroperabilityRecordBundle> getMy(Authentication auth) {
        //TODO: Implement
        List<InteroperabilityRecordBundle> re = new ArrayList<>();
        return re;
    }

    private Resource getPendingResourceViaInteroperabilityRecordId(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

}
