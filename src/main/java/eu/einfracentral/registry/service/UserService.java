package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.service.ParserService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 07/08/17.
 */
@Service("userService")
public interface UserService extends ResourceCRUDService<User> {
    String login(User credentials);
	//TODO: maybe instead of String token, return a proper object describing auth?
    void activate(String id);

    void register(User user);
//
//    void register(String body);
}
