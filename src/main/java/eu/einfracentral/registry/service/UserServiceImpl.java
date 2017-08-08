package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import java.util.Date;

/**
 * Created by pgl on 07/08/17.
 */
@org.springframework.stereotype.Service("userService")
public class UserServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<User> implements UserService {

    public UserServiceImpl() {
        super(User.class);
    }

    @Override
    public String getResourceType() {
        return "eInfraUser";
    }

    @Override
    public User login(User creds) {
        throw new Error("User login not yet implemented!");
    }

    @Override
    public void activate(String token) {
        User ret = get(token);
        if (ret.getJoin_date() != null) {
            ret.setJoin_date(new Date().toString());
        }
    }
}