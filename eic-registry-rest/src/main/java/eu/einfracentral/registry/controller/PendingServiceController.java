package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("pendingService")
public class PendingServiceController extends ResourceController<InfraService, Authentication> {

    private final PendingServiceService pendingServiceManager;

    @Autowired
    PendingServiceController(PendingServiceService pendingServiceManager) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
    }

    @PostMapping("/addService")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Service addService (@RequestBody Service service, @ApiIgnore Authentication auth){
        InfraService infraService = new InfraService(service);
        return pendingServiceManager.add(infraService, auth).getService();
    }

    @PostMapping("/transformToPending")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToPending (@RequestParam String serviceId){
        pendingServiceManager.transformToPendingService(serviceId);
    }

    @PostMapping("/transformToInfra")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToInfra (@RequestParam String serviceId){
        pendingServiceManager.transformToInfraService(serviceId);
    }

}
