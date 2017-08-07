package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by pgl on 07/08/17.
 */
@RestController
@RequestMapping("user")
public class UserController extends GenericRestController<User> {
    final private UserService userService;

    @Autowired
    UserController(UserService service) {
        super(service);
        this.userService = service;
    }
}