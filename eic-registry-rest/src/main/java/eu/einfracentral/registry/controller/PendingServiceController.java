package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.PendingResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("pendingService")
public class PendingServiceController extends ResourceController<InfraService, Authentication> {

    private final PendingResourceService<InfraService> pendingServiceManager;

    @Autowired
    PendingServiceController(PendingResourceService<InfraService> pendingServiceManager) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
    }

    @PostMapping("/addService")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Service addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = new InfraService(service);
        return pendingServiceManager.add(infraService, auth).getService();
    }

    @PostMapping("/transformToPending")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToPending(@RequestParam String serviceId) {
        pendingServiceManager.transformToPending(serviceId);
    }

    @PostMapping("/transformToInfra")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToInfra(@RequestParam String serviceId) {
        pendingServiceManager.transformToActive(serviceId);
    }

}
