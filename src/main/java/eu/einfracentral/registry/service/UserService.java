package eu.einfracentral.registry.service;

import eu.einfracentral.domain.User;
import org.springframework.security.core.Authentication;

public interface UserService extends ResourceService<User, Authentication> {


    User activate(String id);


    User reset(User user);


    User register(User user);


    User forgot(String email);


    String getToken(User user);


    boolean authenticate(User credentials);


    User getUserByEmail(String email);
}
