package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ResourceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("pendingServiceManager")
public class PendingServiceManager extends ResourceManager<InfraService> implements ResourceService<InfraService, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingServiceManager.class);

    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    public PendingServiceManager(InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(InfraService.class);
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "pending_service";
    }

    @Override
    public InfraService add(InfraService service, Authentication auth) {

        service.setId(eu.einfracentral.domain.Service.createId(service.getService()));

        if (service.getMetadata() == null) {
            service.setMetadata(Metadata.createMetadata(new User(auth).getFullName()));
        }
        service.setActive(true);
        service.setLatest(true);

        super.add(service, auth);

        return service;
    }

//    public InfraService update(InfraService service, Authentication auth) {
//        Resource existing;
//        try {
//            existing = infraServiceService.getResource(service.getService().getId(), service.getService().getVersion());
//            if (existing == null) {
//                logger.warn("Could not find Service with id '{}' and version '{}'. Attempting search without version",
//                        service.getService().getId(), service.getService().getVersion());
//                existing = infraServiceService.getResource(service.getService().getId(), null);
//                if (existing == null) {
//                    throw new ResourceNotFoundException("Could not find service with id: " + service.getId());
//                }
//            }
//
//            existing.setPayload(serialize(service));
//            existing.setResourceTypeName(resourceType.getName());
//            existing.setResourceType(null);
//            resourceService.updateResource(existing);
//            logger.debug("Moving Service to Pending Service: {}", service);
//
//        } catch (RuntimeException e) {
//            logger.error("Could not change resource type to service: {}", service, e);
//            return service;
//        }
//
//        return service;
//    }

}
