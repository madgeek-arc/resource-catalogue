package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import org.springframework.stereotype.Service;

@Service("eventService")
public class EventManager extends ResourceManager<Event> implements EventService {
    public EventManager() {
        super(Event.class);
    }

    @Override
    public String getResourceType() {
        return "event";
    }
}
