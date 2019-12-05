package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("pendingServiceManager")
public class PendingServiceManager extends ResourceManager<InfraService> implements PendingResourceService<InfraService> {

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

    @Override
    public void transformToPending(String serviceId) {
        InfraService service = infraServiceService.get(serviceId);
        Resource resource = infraServiceService.getResource(service.getService().getId(), service.getService().getVersion());
        resource.setResourceTypeName("infra_service");
        resourceService.changeResourceType(resource, resourceType);
    }

    @Override
    public void transformToActive(String serviceId) {
        infraServiceService.validate(get(serviceId));
        ResourceType infraResourceType = resourceTypeService.getResourceType("infra_service");
        Resource resource = getResource(serviceId);
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);
    }
}
