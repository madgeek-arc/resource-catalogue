package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Manager;
import eu.einfracentral.registry.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 08/01/18.
 */
@RestController
@RequestMapping("manager")
public class ManagerController extends ResourceController<Manager> {
    @Autowired
    ManagerController(ManagerService service) {
        super(service);
    }
}
