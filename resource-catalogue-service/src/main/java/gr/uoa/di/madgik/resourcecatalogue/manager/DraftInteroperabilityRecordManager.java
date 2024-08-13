package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.*;

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
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle bundle, Authentication auth) {

        bundle.setId(idCreator.generate(getResourceType()));

        logger.trace("Attempting to add a new Draft Interoperability Record with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        bundle.getInteroperabilityRecord().setCatalogueId(catalogueId);
        bundle.setActive(false);
        bundle.setDraft(true);

        super.add(bundle, auth);

        return bundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle bundle, Authentication auth) {
        // get existing resource
        Resource existing = getDraftResource(bundle.getInteroperabilityRecord().getId());
        // block catalogueId updates from Provider Admins
        bundle.getInteroperabilityRecord().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Interoperability Record with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(bundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Interoperability Record: {}", bundle);
        return bundle;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(InteroperabilityRecordBundle bundle) {
        super.delete(bundle);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle transformToNonDraft(String id, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = this.get(id);
        return transformToNonDraft(interoperabilityRecordBundle, auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle transformToNonDraft(InteroperabilityRecordBundle bundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Interoperability Record with id {} to Active", bundle.getId());
        interoperabilityRecordService.validate(bundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        bundle.setStatus("pending interoperability record");
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        bundle.setDraft(false);

        ResourceType guidelinesResourceType = resourceTypeService.getResourceType("interoperability_record");
        Resource resource = getDraftResource(bundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, guidelinesResourceType);

        try {
            bundle = interoperabilityRecordService.update(bundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return bundle;
    }

    public List<InteroperabilityRecordBundle> getMy(Authentication auth) {
        //TODO: Implement
        List<InteroperabilityRecordBundle> re = new ArrayList<>();
        return re;
    }

    private Resource getDraftResource(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

}
