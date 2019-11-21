package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.PendingService;
import eu.einfracentral.registry.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pendingService")
public class PendingServiceController extends ResourceController<PendingService, Authentication> {

    @Autowired
    PendingServiceController(ResourceService<PendingService, Authentication> service) {
        super(service);
    }

}
