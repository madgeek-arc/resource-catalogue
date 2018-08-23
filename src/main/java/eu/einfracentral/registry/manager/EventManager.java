package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unchecked")
public class EventManager extends ResourceManager<Event> implements EventService {

    private static final Logger logger = LogManager.getLogger(EventManager.class);

    public EventManager() {
        super(Event.class);
    }

    @Autowired
    private ParserService parserService;

    @Override
    public String getResourceType() {
        return "event";
    }

    @Override
    public Event add(Event event) {
        event.setId(UUID.randomUUID().toString());
        event.setInstant(System.currentTimeMillis());
        return super.add(event);
    }

    public Event update(Event event) {
        event.setInstant(System.currentTimeMillis());
        return super.update(event);
    }

    public void delete(Event event) {
        super.delete(event);
    }

    @Override
    public Event toggleFavourite(String serviceId, String userId) {
        List<Event> events = getEvents(Event.UserActionType.FAVOURITE.getKey(), serviceId, userId);
        Event event;
        if (events.size() == 0) {
            event = new Event();
            event.setService(serviceId);
            event.setUser(userId);
            event.setType(Event.UserActionType.FAVOURITE.getKey());
            event.setValue("1");
            event = add(event);
        } else {
            event = events.get(0);
            event = booleanToggleValue(event);
            event = update(event);
        }
        return event;
    }

    @Override
    public Event setRating(String serviceId, String userId, String value) {
        List<Event> events = getEvents(Event.UserActionType.RATING.getKey(), serviceId, userId);
        Event event;
        if (events.size() == 0) {
            event = new Event();
            event.setService(serviceId);
            event.setUser(userId);
            event.setType(Event.UserActionType.RATING.getKey());
            event.setValue(value);
            event = add(event);
        } else {
            event = events.get(0);
            event.setValue(value);
            event = update(event);
        }
        return event;
    }

    @Override
    public List<Event> getEvents(String eventType) {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType, "event");
        return pagingToList(event_resources);
    }

    @Override
    public List<Event> getEvents(String eventType, String serviceId, String userId) {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType + " AND service=" + serviceId + " AND event_user=" + userId, "event");
        return pagingToList(event_resources);
    }

    @Override
    public List<Event> getServiceEvents(String eventType, String serviceId) {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType + " AND service=" + serviceId, "event");
        return pagingToList(event_resources);
    }

    @Override
    public List<Event> getUserEvents(String eventType, String userId) {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType + " AND event_user=" + userId, "event");
        return pagingToList(event_resources);
    }

    private Event booleanToggleValue(Event event) {
        if ("1".equals(event.getValue())) {
            event.setValue("0");
        } else {
            event.setValue("1");
        }
        return event;
    }

    private List<Event> pagingToList(Paging<Resource> resources) {
        List<Event> events = resources.getResults().stream().map(resource -> {
            try {
                return parserService.deserialize(resource, Event.class).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        logger.info(events.toString());
        return events;
    }
}
