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
        logger.info("User '{}' attempting to add a new Event '{}' with type '{}'", authentication.getName(), event, event.getType());
        return new ResponseEntity<>(eventService.add(event, authentication), HttpStatus.CREATED);
    }

    @Override
    @PutMapping(path = "updateEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> update(@RequestBody Event event, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        logger.info("User '{}' attempting to update Event '{}' ", authentication.getName(), event);
        return new ResponseEntity<>(eventService.update(event, authentication), HttpStatus.OK);
    }

    @Override
    @DeleteMapping(path = "deleteEvent", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> delete(@RequestBody Event event, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        logger.info("User '{}' attempting to delete Event '{}' ", authentication.getName(), event);
        eventService.delete(event);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(path = "addVisitsOnDay", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addVisitsOnDay(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date, @RequestParam String serviceId,
                               @RequestParam Float noOfVisits, Authentication auth) {
        logger.info("User '{}' attempting to add '{}' visits on date '{}' for service '{}'", auth.getName(), noOfVisits, date, serviceId);
        eventService.addVisitsOnDay(date, serviceId, noOfVisits, auth);
    }

    @PostMapping(path = "visits/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setVisit(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                          @RequestParam("visit") Float visit) throws Exception {
        String serviceId = prefix + "/" + suffix;
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setVisit(serviceId, visit), HttpStatus.OK);
        logger.info("Someone Visited Service with id '{}'", serviceId);
        return ret;
    }

    @PostMapping(path = "addToProject/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setAddToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                 @RequestParam("addToProject") Float addToProject) throws Exception {
        String id = prefix + "/" + suffix;
        ResponseEntity<Event> ret = new ResponseEntity<>(eventService.setAddToProject(id, addToProject), HttpStatus.OK);
        logger.info("Someone Added To Project Service with id '{}'", id);
        return ret;
    }

    @PostMapping(path = "order/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setOrder(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                          @RequestParam("order") Float order) throws Exception {
        String id = prefix + "/" + suffix;
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

    @GetMapping(path = "visit/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceVisits(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.VISIT.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "addToProject/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceAddToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.ADD_TO_PROJECT.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "order/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getServiceOrders(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.ORDER.getKey(), id), HttpStatus.OK);
    }

    @GetMapping(path = "aggregate/visits/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedVisits(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return eventService.getServiceAggregatedVisits(id);
    }

    @GetMapping(path = "aggregate/addToProject/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedAddToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return eventService.getServiceAggregatedAddToProject(id);
    }

    @GetMapping(path = "aggregate/order/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public int getServiceAggregatedOrders(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return eventService.getServiceAggregatedOrders(id);
    }
}