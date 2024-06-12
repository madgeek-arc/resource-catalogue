package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import gr.uoa.di.madgik.resourcecatalogue.service.EventService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("event")
@Tag(name = "event")
public class EventController extends ResourceController<Event> {

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
    public ResponseEntity<Event> get(@PathVariable String id, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(eventService.get(id), HttpStatus.OK);
    }

    @Override
    @PostMapping(path = "addEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> add(@RequestBody Event event, @Parameter(hidden = true) Authentication authentication) {
        logger.info("Attempting to add a new Event '{}' with type '{}'", event, event.getType());
        return new ResponseEntity<>(eventService.add(event, authentication), HttpStatus.CREATED);
    }

    @Override
    @PutMapping(path = "updateEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> update(@RequestBody Event event, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        logger.info("Attempting to update Event '{}' ", event);
        return new ResponseEntity<>(eventService.update(event, authentication), HttpStatus.OK);
    }

    @Override
    @DeleteMapping(path = "deleteEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> delete(@RequestBody Event event, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        logger.info("Attempting to delete Event '{}' ", event);
        eventService.delete(event);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(path = "addVisitsOnDay", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addVisitsOnDay(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date, @RequestParam String serviceId,
                               @RequestParam Float noOfVisits, Authentication auth) {
        logger.info("Attempting to add '{}' visits on date '{}' for service '{}'", noOfVisits, date, serviceId);
        eventService.addVisitsOnDay(date, serviceId, noOfVisits, auth);
    }

    @PostMapping(path = "visits/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setVisit(@PathVariable String id, @RequestParam("visit") Float visit) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setVisit(id, visit), HttpStatus.OK);
        logger.info("Someone Visited Service with id '{}'", id);
        return ret;
    }

    @PostMapping(path = "addToProject/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setAddToProject(@PathVariable String id, @RequestParam("addToProject") Float addToProject) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setAddToProject(id, addToProject), HttpStatus.OK);
        logger.info("Someone Added To Project Service with id '{}'", id);
        return ret;
    }

    @PostMapping(path = "order/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setOrder(@PathVariable String id, @RequestParam("order") Float order) throws Exception {
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setOrder(id, order), HttpStatus.OK);
        logger.info("Someone Ordered Service with id '{}'", id);
        return ret;
    }

    @GetMapping(path = "visits/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getAllVisits() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.VISIT.getKey()), HttpStatus.OK);
    }

    @GetMapping(path = "addToProject/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getAllAddToProject() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.ADD_TO_PROJECT.getKey()), HttpStatus.OK);
    }

    @GetMapping(path = "order/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getOrders() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.ORDER.getKey()), HttpStatus.OK);
    }

    @GetMapping(path = "visit/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceVisits(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.VISIT.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "addToProject/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceAddToProject(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.ADD_TO_PROJECT.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "order/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceOrders(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.ORDER.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "aggregate/visits/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedVisits(@PathVariable String id) {
        return eventService.getServiceAggregatedVisits(id);
    }

    @GetMapping(path = "aggregate/addToProject/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedAddToProject(@PathVariable String id) {
        return eventService.getServiceAggregatedAddToProject(id);
    }

    @GetMapping(path = "aggregate/order/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedOrders(@PathVariable String id) {
        return eventService.getServiceAggregatedOrders(id);
    }
}