package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.service.IdCreator;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("pendingServiceManager")
public class PendingServiceManager extends ResourceManager<InfraService> implements PendingResourceService<InfraService> {

    private static final Logger logger = LogManager.getLogger(PendingServiceManager.class);

    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final PendingProviderManager pendingProviderManager;
    private final IdCreator idCreator;
    private final ProviderManager providerManager;

    @Autowired
    public PendingServiceManager(InfraServiceService<InfraService, InfraService> infraServiceService,
                                 PendingProviderManager pendingProviderManager,
                                 IdCreator idCreator, ProviderManager providerManager) {
        super(InfraService.class);
        this.infraServiceService = infraServiceService;
        this.pendingProviderManager = pendingProviderManager;
        this.idCreator = idCreator;
        this.providerManager = providerManager;
    }

    @Override
    public String getResourceType() {
        return "pending_service";
    }

    @Override
    public InfraService add(InfraService service, Authentication auth) {

        service.setId(idCreator.createServiceId(service.getService()));

        if (service.getMetadata() == null) {
            service.setMetadata(Metadata.createMetadata(new User(auth).getFullName()));
        }
        service.setActive(true);
        service.setLatest(true);

        super.add(service, auth);

        return service;
    }

    @Override
    public InfraService update(InfraService infraService, Authentication auth) {
//        for (String providerId : infraService.getService().getProviders()){
//            ProviderBundle providerBundle = pendingProviderManager.getOrNull(providerId);
//            if (providerBundle == null){
//                providerBundle = providerManager.get(providerId, auth);
//                providerManager.changeProviderStateForPendingServices(providerBundle);
//            } else {
//                pendingProviderManager.changePendingProviderState(providerBundle);
//            }
//        }
        infraService.setMetadata(Metadata.updateMetadata(infraService.getMetadata(), new User(auth).getFullName()));
        // get existing resource
        Resource existing = whereID(infraService.getId(), true);
        // save existing resource with new payload
        existing.setPayload(serialize(infraService));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating PendingService: {}", infraService);
        return infraService;
    }

    @Override
    public InfraService transformToPending(InfraService infraService, Authentication auth) {
        return transformToPending(infraService.getId(), auth);
    }

    @Override
    public InfraService transformToPending(String serviceId, Authentication auth) {
        InfraService infraService = infraServiceService.get(serviceId);
        Resource resource = infraServiceService.getResource(infraService.getService().getId(), infraService.getService().getVersion());
        resource.setResourceTypeName("infra_service");
        resourceService.changeResourceType(resource, resourceType);
        return infraService;
    }

    @Override
    public InfraService transformToActive(InfraService infraService, Authentication auth) {
        infraServiceService.validate(infraService);
        infraService = update(infraService, auth);
        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
        Resource resource = getResource(infraService.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);
        return infraService;
    }

    @Override
    public InfraService transformToActive(String serviceId, Authentication auth) {
        InfraService infraService = get(serviceId);
        infraServiceService.validate(infraService);
        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
        Resource resource = getResource(serviceId);
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

}
