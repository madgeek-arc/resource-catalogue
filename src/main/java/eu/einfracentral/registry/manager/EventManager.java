package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import java.util.UUID;
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

    public Event update(Event event) {
        return super.update(event);
    }

    public void delete(Event event) {
        super.delete(event);
    }
}
