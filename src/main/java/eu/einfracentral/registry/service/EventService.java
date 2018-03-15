package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Event;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 05/01/18.
 */
@Service("actionService")
public interface EventService extends ResourceService<Event> {
}
