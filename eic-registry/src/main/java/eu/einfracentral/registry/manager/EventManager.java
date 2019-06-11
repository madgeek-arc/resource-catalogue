package eu.einfracentral.registry.manager;

import com.google.i18n.phonenumbers.NumberParseException;
import eu.einfracentral.domain.Event;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.AuthenticationInfo;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_EVENTS;

@Component
@SuppressWarnings("unchecked")
public class EventManager extends ResourceManager<Event> implements EventService {

    private static final Logger logger = LogManager.getLogger(EventManager.class);
    private ParserService parserService;
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    public EventManager(ParserService parserService, @Lazy InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(Event.class);
        this.parserService = parserService;
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "event";
    }

    @Override
    public void deleteEvents(List<Event> events) {
        if (!events.isEmpty()) {
            for (Event event : events) {
                this.delete(event);
                logger.info(String.format("Deleting Event:%n-id: %s%n-Service: %s%n-Type: %s", event.getId(), event.getService(), event.getType()));
            }
        }
    }

    @Override
    @CacheEvict(value = CACHE_EVENTS, allEntries = true)
    public Event add(Event event, Authentication auth) {
        event.setId(UUID.randomUUID().toString());
        event.setInstant(System.currentTimeMillis());
        Event ret = super.add(event, auth);
        logger.info("Adding Event " + event);
        return ret;
    }

    @Override
    @CacheEvict(value = CACHE_EVENTS, allEntries = true)
    public Event update(Event event, Authentication auth) {
        event.setInstant(System.currentTimeMillis());
        Event ret = super.update(event, auth);
        logger.info("Updating Event " + event);
        return ret;
    }

    @Override
    @CacheEvict(value = CACHE_EVENTS, allEntries = true)
    public Event setFavourite(String serviceId, Boolean value, Authentication authentication) throws ResourceNotFoundException {
        if (!infraServiceService.exists(new SearchService.KeyValue("infra_service_id", serviceId))) {
            throw new ResourceNotFoundException("infra_service", serviceId);
        }
        String favouriteValue = value ? "1" : "0";
        List<Event> events = getEvents(Event.UserActionType.FAVOURITE.getKey(), serviceId, authentication);
        Event event;
        if (!events.isEmpty() && sameDay(events.get(0).getInstant())) {
            event = events.get(0);
            delete(event);
            logger.debug("Deleting previous FAVORITE Event " + event + " because it happened more than once in the same day.");
        } else {
            event = new Event();
            event.setService(serviceId);
            event.setUser(AuthenticationInfo.getSub(authentication));
            event.setType(Event.UserActionType.FAVOURITE.getKey());
            event.setValue(favouriteValue);
            event = add(event, null);
            logger.debug("Adding a new FAVORITE Event " + event);
        }
        return event;
    }

    @Override
    @CacheEvict(value = CACHE_EVENTS, allEntries = true)
    public Event setRating(String serviceId, String value, Authentication authentication) throws ResourceNotFoundException, NumberParseException {
        if (!infraServiceService.exists(new SearchService.KeyValue("infra_service_id", serviceId))) {
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
            logger.debug("Updating RATING Event " + event);
        } else {
            event = new Event();
            event.setService(serviceId);
            event.setUser(AuthenticationInfo.getSub(authentication));
            event.setType(Event.UserActionType.RATING.getKey());
            event.setValue(value);
            event = add(event, null);
            logger.debug("Adding a new RATING Event " + event);
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
    @Cacheable(value = CACHE_EVENTS)
    public List<Event> getEvents(String eventType, String serviceId, Authentication authentication) {
        if (authentication == null) {
            return new ArrayList<>();
        }
        Paging<Resource> eventResources = searchService.cqlQuery(
                String.format("type=\"%s\" AND service=\"%s\" AND event_user=\"%s\"",
                        eventType, serviceId, AuthenticationInfo.getSub(authentication)), getResourceType(),
                10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
    }

    @Override
    @Cacheable(value = CACHE_EVENTS)
    public List<Event> getServiceEvents(String eventType, String serviceId) {
        Paging<Resource> eventResources = searchService.cqlQuery(String.format("type=\"%s\" AND service=\"%s\"",
                eventType, serviceId), getResourceType(), 10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
    }

    @Override
    @Cacheable(value = CACHE_EVENTS)
    public List<Event> getUserEvents(String eventType, Authentication authentication) {
        if (authentication == null) {
            return new ArrayList<>();
        }
        Paging<Resource> eventResources = searchService.cqlQuery(String.format("type=\"%s\" AND event_user=\"%s\"",
                eventType, AuthenticationInfo.getSub(authentication)), getResourceType(),
                10000, 0, "creation_date", "DESC");
        return pagingToList(eventResources);
    }

    @Override
    @Cacheable(value = CACHE_EVENTS)
    public Map<String, List<Float>> getAllServiceEventValues(String eventType, Authentication authentication) {
        Map<String, List<Float>> allServiceEvents = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", eventType);
        Map<String, Object> order = new HashMap<>();
        Map<String, Object> sort = new HashMap<>();
        order.put("order", "desc");
        sort.put("instant", order);
        ff.setOrderBy(sort);
        List<Event> events = getAll(ff, authentication).getResults();
        List<String> userList = events.stream().map(Event::getUser).distinct().collect(Collectors.toList());
        List<String> serviceList = events.stream().map(Event::getService).distinct().collect(Collectors.toList());

        // for each service
        for (String service : serviceList) {
            List<Event> serviceEvents = events.stream()
                    .filter(e -> e.getService().equals(service))
                    .collect(Collectors.toList());

            Map<String, Event> userEventsMap = new HashMap<>();

            // for each event, save only the latest user events (events order is descending on field 'instant')
            for (Event event : serviceEvents) {
                userEventsMap.putIfAbsent(event.getUser(), event);
            }

            List<Float> eventsValues = userEventsMap.values()
                    .stream()
                    .map(Event::getValue)
                    .map(Float::parseFloat)
                    .collect(Collectors.toList());
            allServiceEvents.put(service, eventsValues);
        }

        return allServiceEvents;
    }

    private List<Event> pagingToList(Paging<Resource> resources) {
        return resources.getResults()
                .stream()
                .map(resource -> parserService.deserialize(resource, Event.class))
                .collect(Collectors.toList());
    }

    private boolean sameDay(Long instant) {
        Calendar midnight = new GregorianCalendar();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        return midnight.getTimeInMillis() < instant;
    }
}
