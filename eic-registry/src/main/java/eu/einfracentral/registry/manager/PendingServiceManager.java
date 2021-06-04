package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.*;

@Service("pendingServiceManager")
public class PendingServiceManager extends ResourceManager<InfraService> implements PendingResourceService<InfraService> {

    private static final Logger logger = LogManager.getLogger(PendingServiceManager.class);

    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final IdCreator idCreator;
    private final SecurityService securityService;

    @Autowired
    public PendingServiceManager(InfraServiceService<InfraService, InfraService> infraServiceService,
                                 IdCreator idCreator, @Lazy SecurityService securityService) {
        super(InfraService.class);
        this.infraServiceService = infraServiceService;
        this.idCreator = idCreator;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "pending_service";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService add(InfraService service, Authentication auth) {

        service.setId(idCreator.createServiceId(service.getService()));
        logger.trace("User '{}' is attempting to add a new Pending Service with id {}", auth, service.getId());

        if (service.getMetadata() == null) {
            service.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        if (service.getLoggingInfo() == null){
            LoggingInfo loggingInfo = LoggingInfo.createLoggingInfo(User.of(auth).getEmail(), securityService.getRoleName(auth));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            loggingInfoList.add(loggingInfo);
            service.setLoggingInfo(loggingInfoList);
        }

        service.setActive(true);
        service.setLatest(true);

        super.add(service, auth);

        return service;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService update(InfraService infraService, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Pending Service with id {}", auth, infraService.getId());
        infraService.setMetadata(Metadata.updateMetadata(infraService.getMetadata(), User.of(auth).getFullName()));
        // get existing resource
        Resource existing = this.whereID(infraService.getId(), true);
        // save existing resource with new payload
        existing.setPayload(serialize(infraService));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating PendingService: {}", infraService);
        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService transformToPending(InfraService infraService, Authentication auth) {
        return transformToPending(infraService.getId(), auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService transformToPending(String serviceId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Active Service with id {} to Pending", auth, serviceId);
        InfraService infraService = infraServiceService.get(serviceId);
        Resource resource = infraServiceService.getResource(infraService.getService().getId(), infraService.getService().getVersion());
        resource.setResourceTypeName("infra_service");
        resourceService.changeResourceType(resource, resourceType);
        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService transformToActive(InfraService infraService, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Service with id {} to Active", auth, infraService.getId());
        infraServiceService.validate(infraService);
        infraServiceService.validateCategories(infraService.getService().getCategories());
        infraServiceService.validateScientificDomains(infraService.getService().getScientificDomains());
        infraService = this.update(infraService, auth);
        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
        Resource resource = this.getResource(infraService.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);
        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService transformToActive(String serviceId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Service with id {} to Active", auth, serviceId);
        InfraService infraService = this.get(serviceId);
        infraServiceService.validate(infraService);
        infraServiceService.validateCategories(infraService.getService().getCategories());
        infraServiceService.validateScientificDomains(infraService.getService().getScientificDomains());
        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
        Resource resource = this.getResource(serviceId);
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);
        return infraService;
    }

    public Object getPendingRich(String id, Authentication auth) {
        return infraServiceService.createRichService(get(id), auth);
    }

    public List<InfraService> getMy(Authentication auth) {
        List<InfraService> re = new ArrayList<>();
        return re;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth){
        return true;
    }

    public void adminAcceptedTerms(String providerId, Authentication auth){
        // We need this method on PendingProviderManager. Both PendingManagers share the same Service - PendingResourceService
    }
}
