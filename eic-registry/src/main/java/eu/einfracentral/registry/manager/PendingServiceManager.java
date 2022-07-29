package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.*;

@Service("pendingServiceManager")
public class PendingServiceManager extends ResourceManager<ServiceBundle> implements PendingResourceService<ServiceBundle> {

    private static final Logger logger = LogManager.getLogger(PendingServiceManager.class);

    private final InfraServiceService<ServiceBundle, ServiceBundle> infraServiceService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderManager providerManager;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public PendingServiceManager(InfraServiceService<ServiceBundle, ServiceBundle> infraServiceService,
                                 IdCreator idCreator, @Lazy SecurityService securityService, @Lazy VocabularyService vocabularyService,
                                 @Lazy ProviderManager providerManager) {
        super(ServiceBundle.class);
        this.infraServiceService = infraServiceService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.providerManager = providerManager;
    }

    @Override
    public String getResourceType() {
        return "pending_service";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle add(ServiceBundle service, Authentication auth) {

        service.setId(idCreator.createServiceId(service.getService()));

        // Check if there is a Resource with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<ServiceBundle> resourceList = infraServiceService.getAll(ff, auth).getResults();
        for (ServiceBundle existingResource : resourceList){
            if (service.getService().getId().equals(existingResource.getService().getId()) && existingResource.getService().getCatalogueId().equals(catalogueName)) {
                throw new ValidationException("Resource with the specific id already exists on the EOSC Catalogue. Please refactor your 'name' and/or 'abbreviation' field.");
            }
        }
        logger.trace("User '{}' is attempting to add a new Pending Service with id {}", auth, service.getId());

        if (service.getMetadata() == null) {
            service.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        if (service.getLoggingInfo() == null){
            LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.DRAFT.getKey(), LoggingInfo.ActionType.CREATED.getKey());
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            loggingInfoList.add(loggingInfo);
            service.setLoggingInfo(loggingInfoList);
        }

        service.getService().setCatalogueId(catalogueName);
        service.setActive(false);

        super.add(service, auth);

        return service;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle update(ServiceBundle serviceBundle, Authentication auth) {
        // block catalogueId updates from Provider Admins
        serviceBundle.getService().setCatalogueId(catalogueName);
        logger.trace("User '{}' is attempting to update the Pending Service with id {}", auth, serviceBundle.getId());
        serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), User.of(auth).getFullName()));
        // get existing resource
//        Resource existing = this.whereID(serviceBundle.getId(), true);
        Resource existing = this.getPendingResource(serviceBundle.getService().getId(), serviceBundle.getService().getVersion());
        if (existing == null){
            existing = this.getPendingResource(serviceBundle.getService().getId(), null);
        }
        // save existing resource with new payload
        existing.setPayload(serialize(serviceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating PendingService: {}", serviceBundle);
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
        ServiceBundle serviceBundle = infraServiceService.get(serviceId);
        Resource resource = infraServiceService.getResource(serviceBundle.getService().getId(), catalogueName);
        resource.setResourceTypeName("infra_service");
        resourceService.changeResourceType(resource, resourceType);
        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToActive(ServiceBundle serviceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Service with id {} to Active", auth, serviceBundle.getId());
        infraServiceService.validate(serviceBundle);

        // update loggingInfo
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList  = new ArrayList<>();
        if (serviceBundle.getLoggingInfo() != null) {
            loggingInfoList = serviceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        serviceBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerManager.get(serviceBundle.getService().getResourceOrganisation()).getTemplateStatus().equals("approved template")){
            serviceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            serviceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        serviceBundle = this.update(serviceBundle, auth);
        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
//        Resource resource = this.getResource(serviceBundle.getId());
        Resource resource = this.getPendingResource(serviceBundle.getService().getId(), serviceBundle.getService().getVersion());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            serviceBundle = infraServiceService.update(serviceBundle, auth);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }

        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle transformToActive(String serviceId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Service with id {} to Active", auth, serviceId);
        ServiceBundle serviceBundle = this.get(serviceId);
        infraServiceService.validate(serviceBundle);

        // update loggingInfo
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList  = new ArrayList<>();
        if (serviceBundle.getLoggingInfo() != null) {
            loggingInfoList = serviceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        serviceBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerManager.get(serviceBundle.getService().getResourceOrganisation()).getTemplateStatus().equals("approved template")){
            serviceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            serviceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
//        Resource resource = this.getResource(serviceId);
        Resource resource = this.getPendingResource(serviceId, serviceBundle.getService().getVersion());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            serviceBundle = infraServiceService.update(serviceBundle, auth);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }

        return serviceBundle;
    }

    public Object getPendingRich(String id, Authentication auth) {
        return infraServiceService.createRichService(get(id), auth);
    }

    public List<ServiceBundle> getMy(Authentication auth) {
        List<ServiceBundle> re = new ArrayList<>();
        return re;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth){
        return true;
    }

    public void adminAcceptedTerms(String providerId, Authentication auth){
        // We need this method on PendingProviderManager. Both PendingManagers share the same Service - PendingResourceService
    }

    public Resource getPendingResource(String serviceId, String serviceVersion) {
        Paging<Resource> resources;
        if (serviceVersion == null || "".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("pending_service_id = \"%s\" AND catalogue_id = \"%s\"", serviceId, catalogueName),
                            resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
            // return the latest modified resource that does not contain a version attribute
            for (Resource resource : resources.getResults()) {
                if (!resource.getPayload().contains("<tns:version>")) {
                    return resource;
                }
            }
            if (resources.getTotal() > 0) {
                return resources.getResults().get(0);
            }
            return null;
        } else if ("latest".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("pending_service_id = \"%s\" AND catalogue_id = \"%s\" AND latest = true", serviceId, catalogueName),
                            resourceType.getName(), 1, 0, "modifiedAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("pending_service_id = \"%s\" AND catalogue_id = \"%s\" AND version = \"%s\"", serviceId, catalogueName, serviceVersion), resourceType.getName());
        }
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    public Resource getPendingResource(String providerId) {
        return null;
    }
}
