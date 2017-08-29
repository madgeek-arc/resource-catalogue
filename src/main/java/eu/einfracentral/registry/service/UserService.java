package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 07/08/17.
 */
@Service("userService")
public interface UserService extends ResourceCRUDService<User> {

    User activate(String id);

    User register(User user);

    String getToken(User user);

    boolean authenticate(User credentials);
}
