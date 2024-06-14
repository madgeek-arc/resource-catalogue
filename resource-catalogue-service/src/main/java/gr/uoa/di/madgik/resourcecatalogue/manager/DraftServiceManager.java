package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
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

@Service("draftServiceManager")
public class DraftServiceManager extends ResourceManager<ServiceBundle> implements DraftResourceService<ServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftServiceManager.class);

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftServiceManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                               IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                               @Lazy ProviderService providerService,
                               ProviderResourcesCommonMethods commonMethods) {
        super(ServiceBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "draft_service";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle add(ServiceBundle service, Authentication auth) {

        service.setId(idCreator.generate(getResourceType()));

        // Check if there is a Resource with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ServiceBundle> resourceList = serviceBundleService.getAll(ff, auth).getResults();
        for (ServiceBundle existingResource : resourceList) {
            if (service.getService().getId().equals(existingResource.getService().getId()) && existingResource.getService().getCatalogueId().equals(catalogueId)) {
                throw new ResourceAlreadyExistsException(String.format("Service with the specific id already exists on the [%s] Catalogue.", catalogueId));
            }
        }
        logger.trace("Attempting to add a new Draft Service with id {}", service.getId());

        if (service.getMetadata() == null) {
            service.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        service.setLoggingInfo(loggingInfoList);

        service.getService().setCatalogueId(catalogueId);
        service.setActive(false);

        super.add(service, auth);

        return service;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle update(ServiceBundle serviceBundle, Authentication auth) {
        // get existing resource
        Resource existing = getPendingResourceViaServiceId(serviceBundle.getService().getId());
        // block catalogueId updates from Provider Admins
        serviceBundle.getService().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Service with id {}", serviceBundle.getId());
        serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(serviceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Service: {}", serviceBundle);
        return serviceBundle;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(ServiceBundle serviceBundle) {
        super.delete(serviceBundle);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToNonDraft(String serviceId, Authentication auth) {
        ServiceBundle serviceBundle = this.get(serviceId);
        return transformToNonDraft(serviceBundle, auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToNonDraft(ServiceBundle serviceBundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Service with id {} to Active", serviceBundle.getId());
        serviceBundleService.validate(serviceBundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(serviceBundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerService.get(serviceBundle.getService().getResourceOrganisation()).getTemplateStatus().equals("approved template")) {
            serviceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);
            serviceBundle.setActive(true);
        } else {
            serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }
        serviceBundle.setLoggingInfo(loggingInfoList);
        // latestOnboardingInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType infraResourceType = resourceTypeService.getResourceType("service");
        Resource resource = getPendingResourceViaServiceId(serviceBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            serviceBundle = serviceBundleService.update(serviceBundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return serviceBundle;
    }

    public List<ServiceBundle> getMy(Authentication auth) {
        //TODO: Implement
        List<ServiceBundle> re = new ArrayList<>();
        return re;
    }

    private Resource getPendingResourceViaServiceId(String serviceId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", serviceId, catalogueId),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }
}
