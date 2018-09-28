package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Manager;
import eu.einfracentral.registry.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("manager")
public class ManagerController extends ResourceController<Manager, Authentication> {
    @Autowired
    ManagerController(ManagerService service) {
        super(service);
    }
}
