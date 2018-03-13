package eu.einfracentral.registry.service;

import eu.einfracentral.domain.UserAction;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 05/01/18.
 */
@Service("actionService")
public interface UserActionService extends ResourceService<UserAction> {
}
