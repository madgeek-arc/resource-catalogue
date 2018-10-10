package eu.einfracentral.registry.manager;

import com.google.i18n.phonenumbers.NumberParseException;
import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.utils.AuthenticationDetails;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.GregorianCalendar;
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

    @Autowired
    private InfraServiceManager infraServiceManager;

    @Override
    public String getResourceType() {
        return "event";
    }

    @Override
    public void deleteEvents(List<Event> events) {
        if (!events.isEmpty()) {
            for (Event event : events) {
                this.delete(event);
            }
        }
    }

    @Override
    public Event add(Event event, Authentication auth) {
        event.setId(UUID.randomUUID().toString());
        event.setInstant(System.currentTimeMillis());
        return super.add(event, auth);
    }

    @Override
    public Event update(Event event, Authentication auth) {
        event.setInstant(System.currentTimeMillis());
        return super.update(event, auth);
    }

    @Override
    public Event setFavourite(String serviceId, Boolean value, Authentication authentication) throws ResourceNotFoundException {
        if (!infraServiceManager.exists(new SearchService.KeyValue("infra_service_id", serviceId))) {
            throw new ResourceNotFoundException("infra_service", serviceId);
        }
        String favouriteValue = value ? "1" : "0";
        List<Event> events = getEvents(Event.UserActionType.FAVOURITE.getKey(), serviceId, authentication);
        Event event;
        if (!events.isEmpty() && sameDay(events.get(0).getInstant())) {
            event = events.get(0);
            event.setValue(favouriteValue);
            event = update(event, null);
        } else {
            event = new Event();
            event.setService(serviceId);
            event.setUser(AuthenticationDetails.getSub(authentication));
            event.setType(Event.UserActionType.FAVOURITE.getKey());
            event.setValue(favouriteValue);
            event = add(event, null);
        }
        return event;
    }

    @Override
    public Event setRating(String serviceId, String value, Authentication authentication) throws ResourceNotFoundException, NumberParseException {
        if (!infraServiceManager.exists(new SearchService.KeyValue("infra_service_id", serviceId))) {
            throw new ResourceNotFoundException("infra_service", serviceId);
        }
        if (Long.parseLong(value) <= 0 || Long.parseLong(value) > 5) {
            throw new NumberParseException(NumberParseException.ErrorType.valueOf(value), "Rating value must be between [1,5]");
        }
        List<Event> events = getEvents(Event.UserActionType.RATING.getKey(), serviceId, authentication);
        Event event;
        if (!events.isEmpty() && sameDay(events.get(0).getInstant())) {
            event = events.get(0);
            event.setValue(value);
            event = update(event, null);
        } else {
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
        Paging<Resource> eventResources = searchService
                .cqlQuery(String.format("type=\"%s\"", eventType), getResourceType(),
                        10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
    }

    @Override
    public List<Event> getEvents(String eventType, String serviceId, Authentication authentication) {
        Paging<Resource> eventResources = searchService.cqlQuery(
                String.format("type=\"%s\" AND service=\"%s\" AND event_user=\"%s\"",
                        eventType, serviceId, AuthenticationDetails.getSub(authentication)), getResourceType(),
                10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
    }

    @Override
    public List<Event> getServiceEvents(String eventType, String serviceId) {
        Paging<Resource> eventResources = searchService.cqlQuery(String.format("type=\"%s\" AND service=\"%s\"",
                eventType, serviceId), getResourceType(), 10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
    }

    @Override
    public List<Event> getUserEvents(String eventType, Authentication authentication) {
        Paging<Resource> eventResources = searchService.cqlQuery(String.format("type=\"%s\" AND event_user=\"%s\"",
                eventType, AuthenticationDetails.getSub(authentication)), getResourceType(),
                10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
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
        Calendar midnight = new GregorianCalendar();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        return midnight.getTimeInMillis() < instant;
    }
}
