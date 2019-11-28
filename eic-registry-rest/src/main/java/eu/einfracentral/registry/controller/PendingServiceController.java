package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("pendingService")
public class PendingServiceController extends ResourceController<InfraService, Authentication> {

    private final ResourceService<InfraService, Authentication> pendingServiceManager;

    @Autowired
    PendingServiceController(ResourceService<InfraService, Authentication> pendingServiceManager) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
    }

    @PostMapping("/addService")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Service addService (@RequestBody Service service, @ApiIgnore Authentication auth){
        InfraService infraService = new InfraService(service);
        return pendingServiceManager.add(infraService, auth).getService();
    }

}
