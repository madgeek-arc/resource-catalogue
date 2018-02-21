package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Access;
import eu.einfracentral.registry.service.AccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 05/01/18.
 */
@RestController
@RequestMapping("access")
public class AccessController extends ResourceController<Access> {
    @Autowired
    AccessController(AccessService service) {
        super(service);
    }
}
