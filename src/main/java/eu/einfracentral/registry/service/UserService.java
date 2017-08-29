package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 07/08/17.
 */
@Service("userService")
public interface UserService extends ResourceCRUDService<User> {

    User activate(String id);

    void register(User user);

    String getToken(User user);

//  void register(String body);
}
