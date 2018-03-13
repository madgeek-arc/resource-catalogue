package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.UserAction;
import eu.einfracentral.registry.service.UserActionService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 05/01/18.
 */
@Service("userActionService")
public class UserActionManager extends ResourceManager<UserAction> implements UserActionService {
    public UserActionManager() {
        super(UserAction.class);
    }

    @Override
    public String getResourceType() {
        return "useraction";
    }
}
