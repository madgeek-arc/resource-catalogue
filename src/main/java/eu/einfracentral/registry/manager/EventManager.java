package eu.einfracentral.registry.manager;

import eu.einfracentral.config.security.AuthenticationDetails;
import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Event add(Event event, Authentication auth) {
        event.setId(UUID.randomUUID().toString());
        event.setInstant(System.currentTimeMillis());
        return super.add(event, auth);
    }

    public Event update(Event event, Authentication auth) {
        event.setInstant(System.currentTimeMillis());
        return super.update(event, auth);
    }

    public void delete(Event event) {
        super.delete(event);
    }

    @Override
    public Event setFavourite(String serviceId, Integer value, Authentication authentication) throws Exception {
        List<Event> events = getEvents(Event.UserActionType.FAVOURITE.getKey(), serviceId, authentication);
        if (value != 0) {
            value = 1;
        }
        Event event;
        if (events.size() > 0 && sameDay(events.get(0).getInstant())) {
            event = events.get(0);
            event.setValue(value.toString());
            event = update(event, null);
        } else {
            event = new Event();
            event.setService(serviceId);
            event.setUser(AuthenticationDetails.getSub(authentication));
            event.setType(Event.UserActionType.FAVOURITE.getKey());
            event.setValue(value.toString());
            event = add(event, null);
        }
        return event;
    }

    @Override
    public Event setRating(String serviceId, String value, Authentication authentication) throws Exception {
        List<Event> events = getEvents(Event.UserActionType.RATING.getKey(), serviceId, authentication);
        Event event;
        if (events.size() > 0 && sameDay(events.get(0).getInstant())) {
            event = events.get(0);
            event.setValue(value);
            event = update(event, null);
        }
        else {
            event = new Event();
            event.setService(serviceId);
            event.setUser(AuthenticationDetails.getSub(authentication));
            event.setType(Event.UserActionType.RATING.getKey());
            event.setValue(value);
            event = add(event, null);
        }
        return event;
    }

    @Override
    public List<Event> getEvents(String eventType) {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType, "event", 20000, 0, "creation_date", "DESC");
        return pagingToList(event_resources);
    }

    @Override
    public List<Event> getEvents(String eventType, String serviceId, Authentication authentication) throws Exception {
        Paging<Resource> event_resources = searchService.cqlQuery(
                String.format("type=%s AND service=%s AND event_user=%s",
                        eventType, serviceId, AuthenticationDetails.getSub(authentication)), "event", 20000, 0, "creation_date", "DESC");
        return pagingToList(event_resources);
    }

    @Override
    public List<Event> getServiceEvents(String eventType, String serviceId) {
        Paging<Resource> event_resources = searchService.cqlQuery(String.format("type=%s AND service=%s",
                        eventType, serviceId), "event");
        return pagingToList(event_resources);
    }

    @Override
    public List<Event> getUserEvents(String eventType, Authentication authentication) throws Exception {
        Paging<Resource> event_resources = searchService.cqlQuery(String.format("type=%s AND event_user=%s",
                eventType, AuthenticationDetails.getSub(authentication)), "event", 20000, 0, "creation_date", "DESC");
        return pagingToList(event_resources);
    }

    private List<Event> pagingToList(Paging<Resource> resources) {
        List<Event> events = resources.getResults()
                .stream()
                .map(resource -> parserService.deserialize(resource, Event.class))
                .collect(Collectors.toList());
        logger.info(events.toString());
        return events;
    }

    private boolean sameDay(Long instant) {
        return instant - System.currentTimeMillis() < 86400000;
    }
}
