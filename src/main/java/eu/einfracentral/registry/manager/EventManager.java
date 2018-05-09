package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Event;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.Resource;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EventManager extends ResourceManager<Event> implements EventService {
    public EventManager() {
        super(Event.class);
    }

    @Override
    public String getResourceType() {
        return "event";
    }

    @Override
    public Event add(Event event) {
        event.setId(UUID.randomUUID().toString());
        return super.add(event);
    }
}
