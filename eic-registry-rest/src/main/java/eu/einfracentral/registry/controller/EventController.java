package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("event")
public class EventController extends ResourceController<Event, Authentication> {

    private EventService eventService;

    @Autowired
    EventController(EventService eventService) {
        super(eventService);
        this.eventService = eventService;
    }

    private final Logger logger = LogManager.getLogger(EventController.class);


    // Retrieve the event with a specific ID.
    @GetMapping(path = "event/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Override
    public ResponseEntity<Event> get(@PathVariable String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(eventService.get(id), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(path = "deleteNull/{type}/", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> deleteNullEvents(@PathVariable String type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", type);
        List<Event> events = eventService.getAll(ff, null).getResults();
        List<Event> toDelete = new ArrayList<>();
        for (Event event : events) {
            if (event.getValue() == null) {
                toDelete.add(event);
                logger.info("Attempting delete of null event: {}", event);
            }
        }
        int size = toDelete.size();
        eventService.deleteEvents(toDelete);
        logger.info("Admin deleting null events");
        return new ResponseEntity<>("deleted " + size, HttpStatus.NO_CONTENT);
    }


    // FAVORITES -------->
    // Set a Service as favorite for a user.
    @PostMapping(path = "favourite/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setFavourite(@PathVariable String id, @RequestParam Float value, @ApiIgnore Authentication authentication) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setFavourite(id, value, authentication), HttpStatus.OK);
        if (value == 1) {
            logger.info("User '{}' set Service with id '{}' as FAVORITE", authentication, id);
        } else {
            logger.info("User '{}' set Service with id '{}' as UNFAVORITE", authentication, id);
        }
        return ret;
    }

    // Check if a Service is favourited by the authenticated user.
    @GetMapping(path = "favourite/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Float> getFavourite(@PathVariable String id, @ApiIgnore Authentication authentication) {
        List<Event> events;
        try {
            events = eventService.getEvents(Event.UserActionType.FAVOURITE.getKey(), id, authentication);
            if (!events.isEmpty()) {
                return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.info(String.format("%s\nReturning favourite = 0", e));
        }
        return new ResponseEntity<>(Float.parseFloat("0"), HttpStatus.OK);
    }

    // Retrieve all the favourited events.
    @GetMapping(path = "favourites/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getFavourites() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.FAVOURITE.getKey()), HttpStatus.OK);
    }

    // Retrieve all the favourited events of the authenticated user.
    @GetMapping(path = "favourites", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getUserFavourites(Authentication authentication) {
        return new ResponseEntity<>(eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), authentication), HttpStatus.OK);
    }

    // Retrieve all the favourited events of a infraService with the specified ID.
    @GetMapping(path = "favourites/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceFavourites(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }
    // <-------- FAVORITES


    // RATINGS ---------->
    // Set a rating to a Service from the authenticated user.
    @PostMapping(path = "rating/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setUserRating(@PathVariable String id, @RequestParam("rating") Float rating, @ApiIgnore Authentication authentication) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setRating(id, rating, authentication), HttpStatus.OK);
        logger.info("User '{}' rated Service with id '{}', rating value: {}", authentication, id, rating);
        return ret;
    }

    // Get the rating of the authenticated user.
    @GetMapping(path = "rating/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Float> getRating(@PathVariable String id, @ApiIgnore Authentication authentication) {
        List<Event> events;
        try {
            events = eventService.getEvents(Event.UserActionType.RATING.getKey(), id, authentication);
            if (events != null && !events.isEmpty()) {
                return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.info(e + "\nReturning rate = null");
        }
        return new ResponseEntity<>(Float.parseFloat("null"), HttpStatus.OK);
    }

    // Retrieve all rating events.
    @GetMapping(path = "ratings/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getRatings() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.RATING.getKey()), HttpStatus.OK);
    }

    // Retrieve all the rating events of the authenticated user.
    @GetMapping(path = "ratings", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getUserRating(Authentication authentication) {
        try {
            return new ResponseEntity<>(eventService.getUserEvents(Event.UserActionType.RATING.getKey(), authentication), HttpStatus.OK);
        } catch (Exception e) {
            logger.info(String.format("%s\nReturning ratings = null", e));
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Retrieve all the rating events of a infraService with the specified ID.
    @GetMapping(path = "ratings/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceRatings(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }
    // <---------- RATINGS

    @Override
    @PostMapping(path = "addEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> add(@RequestBody Event event, @ApiIgnore Authentication authentication) {
        logger.info("User '{}' attempting to add a new Event '{}' with type '{}'", authentication.getName(), event, event.getType());
        return new ResponseEntity<>(eventService.add(event, authentication), HttpStatus.CREATED);
    }

    @Override
    @PutMapping(path = "updateEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> update(@RequestBody Event event, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        logger.info("User '{}' attempting to update Event '{}' ", authentication.getName(), event);
        return new ResponseEntity<>(eventService.update(event, authentication), HttpStatus.OK);
    }

    @Override
    @DeleteMapping(path = "deleteEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> delete(@RequestBody Event event, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        logger.info("User '{}' attempting to delete Event '{}' ", authentication.getName(), event);
        eventService.delete(event);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(path = "addVisitsOnDay", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addVisitsOnDay(@RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date date, @RequestParam String serviceId,
                               @RequestParam Float noOfVisits, Authentication auth) {
        logger.info("User '{}' attempting to add '{}' visits on date '{}' for service '{}'", auth.getName(), noOfVisits, date, serviceId);
        eventService.addVisitsOnDay(date, serviceId, noOfVisits, auth);
    }

    @PostMapping(path = "internal/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setInternal(@PathVariable String id, @RequestParam("internal") Float internal) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setInternal(id, internal), HttpStatus.OK);
        logger.info("Someone Internal Viewed Service with id '{}'", id);
        return ret;
    }

    @PostMapping(path = "external/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setExternal(@PathVariable String id, @RequestParam("external") Float external) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setExternal(id, external), HttpStatus.OK);
        logger.info("Someone External Viewed Service with id '{}'", id);
        return ret;
    }

    @PostMapping(path = "order/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setOrder(@PathVariable String id, @RequestParam("order") Float order) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setOrder(id, order), HttpStatus.OK);
        logger.info("Someone Ordered Service with id '{}'", id);
        return ret;
    }

    @GetMapping(path = "internal/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getInternalViews() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.INTERNAL_VIEW.getKey()), HttpStatus.OK);
    }

    @GetMapping(path = "external/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getExternalViews() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.EXTERNAL_VIEW.getKey()), HttpStatus.OK);
    }

    @GetMapping(path = "order/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getOrders() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.ORDER.getKey()), HttpStatus.OK);
    }

    @GetMapping(path = "internal/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceInternals(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.INTERNAL_VIEW.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "external/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceExternals(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.EXTERNAL_VIEW.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "order/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceOrders(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.ORDER.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "aggregate/internal/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedInternals(@PathVariable String id) {
        return eventService.getServiceAggregatedInternals(id);
    }

    @GetMapping(path = "aggregate/external/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedExternals(@PathVariable String id) {
        return eventService.getServiceAggregatedExternals(id);
    }

    @GetMapping(path = "aggregate/order/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedOrders(@PathVariable String id) {
        return eventService.getServiceAggregatedOrders(id);
    }
}
