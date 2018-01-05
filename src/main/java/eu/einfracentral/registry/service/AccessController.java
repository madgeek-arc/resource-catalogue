package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Access;
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
