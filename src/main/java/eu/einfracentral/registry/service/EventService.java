package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Event;
import org.springframework.stereotype.Service;

@Service("eventService")
public interface EventService extends ResourceService<Event> {
}
