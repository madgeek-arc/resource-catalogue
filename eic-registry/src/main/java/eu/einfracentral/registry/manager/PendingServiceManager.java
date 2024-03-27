package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.*;

@Service("pendingServiceManager")
public class PendingServiceManager extends ResourceManager<ServiceBundle> implements PendingResourceService<ServiceBundle> {

    private static final Logger logger = LogManager.getLogger(PendingServiceManager.class);

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderManager providerManager;
    private ServiceBundleManager serviceBundleManager;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public PendingServiceManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                                 IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                                 @Lazy ProviderManager providerManager, @Lazy ServiceBundleManager serviceBundleManager,
                                 ProviderResourcesCommonMethods commonMethods) {
        super(ServiceBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerManager = providerManager;
        this.serviceBundleManager = serviceBundleManager;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "pending_service";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle add(ServiceBundle service, Authentication auth) {

        service.setId(idCreator.createServiceId(service));

        // Check if there is a Resource with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ServiceBundle> resourceList = serviceBundleService.getAll(ff, auth).getResults();
        for (ServiceBundle existingResource : resourceList) {
            if (service.getService().getId().equals(existingResource.getService().getId()) && existingResource.getService().getCatalogueId().equals(catalogueName)) {
                throw new ValidationException(String.format("Service with the specific id already exists on the [%s] Catalogue. Please refactor your 'abbreviation' field.", catalogueName));
            }
        }
        logger.trace("User '{}' is attempting to add a new Pending Service with id {}", auth, service.getId());

        if (service.getMetadata() == null) {
            service.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        service.setLoggingInfo(loggingInfoList);

        service.getService().setCatalogueId(catalogueName);
        service.setActive(false);

        super.add(service, auth);

        return service;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle update(ServiceBundle serviceBundle, Authentication auth) {
        // get existing resource
        Resource existing = this.getPendingResourceViaServiceId(serviceBundle.getService().getId());
        // block catalogueId updates from Provider Admins
        serviceBundle.getService().setCatalogueId(catalogueName);
        logger.trace("User '{}' is attempting to update the Pending Service with id {}", auth, serviceBundle.getId());
        serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(serviceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Pending Service: {}", serviceBundle);
        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToPending(ServiceBundle serviceBundle, Authentication auth) {
        return transformToPending(serviceBundle.getId(), auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToPending(String serviceId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Active Service with id {} to Pending", auth, serviceId);
        ServiceBundle serviceBundle = serviceBundleService.get(serviceId, catalogueName);
        Resource resource = serviceBundleService.getResource(serviceBundle.getService().getId(), catalogueName);
        resource.setResourceTypeName("service");
        resourceService.changeResourceType(resource, resourceType);
        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToActive(ServiceBundle serviceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Service with id {} to Active", auth, serviceBundle.getId());
        serviceBundleService.validate(serviceBundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(serviceBundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerManager.get(serviceBundle.getService().getResourceOrganisation()).getTemplateStatus().equals("approved template")) {
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
        Resource resource = this.getPendingResourceViaServiceId(serviceBundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            serviceBundle = serviceBundleService.update(serviceBundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e);
        }

        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToActive(String serviceId, Authentication auth) {
        ServiceBundle serviceBundle = this.get(serviceId);
        return transformToActive(serviceBundle, auth);
    }

    public List<ServiceBundle> getMy(Authentication auth) {
        List<ServiceBundle> re = new ArrayList<>();
        return re;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth) {
        return true;
    }

    public void adminAcceptedTerms(String providerId, Authentication auth) {
        // We need this method on PendingProviderManager. Both PendingManagers share the same Service - PendingResourceService
    }

    public Resource getPendingResourceViaServiceId(String serviceId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", serviceId, catalogueName),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources.getTotal() > 0) {
            return resources.getResults().get(0);
        }
        return null;
    }

    public Resource getPendingResourceViaProviderId(String providerId) {
        return null;
    }
}
