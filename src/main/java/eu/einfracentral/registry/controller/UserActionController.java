package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.UserAction;
import eu.einfracentral.registry.service.UserActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 05/01/18.
 */
@RestController
@RequestMapping("useraction")
public class UserActionController extends ResourceController<UserAction> {
    @Autowired
    UserActionController(UserActionService service) {
        super(service);
    }
}
