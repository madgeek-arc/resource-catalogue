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
        //this is to clean up the DB from diplastic events (the ones where userids were digits)
        try {
            Integer.parseInt(event.getUser());
        } catch (NumberFormatException nfe) {
            return event;
        }
        //so delete it when the DB is cleared

        event.setId(UUID.randomUUID().toString());
        return super.add(event);
    }
}
