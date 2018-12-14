package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;

import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;

@ApiIgnore
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

    @ApiIgnore
    @ApiOperation("Retrieve all events.")
    @RequestMapping(path = "events/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Event>> getAll(Authentication authentication) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return new ResponseEntity<>(eventService.getAll(ff, authentication), HttpStatus.OK);
    }

    @ApiIgnore
    @ApiOperation("Retrieve the event with a specific ID.")
    @RequestMapping(path = "event/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> get(@PathVariable String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(eventService.get(id), HttpStatus.OK);
    }

    @ApiIgnore
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(path = "deleteNull/{type}/", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> deleteNullEvents(@PathVariable String type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", type);
        List<Event> events = eventService.getAll(ff, null).getResults();
        List<Event> toDelete = new ArrayList<>();
        for (Event event : events) {
            if (event.getValue() == null) {
                toDelete.add(event);
            }
        }
        int size = toDelete.size();
        eventService.deleteEvents(toDelete);
        return new ResponseEntity<>("deleted " + size, HttpStatus.NO_CONTENT);
    }


    // FAVORITES -------->
    @ApiOperation("Set a Service as favorite for a user.")
    @RequestMapping(path = "favourite/service/{id}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> setFavourite(@PathVariable String id, @RequestParam boolean value, @ApiIgnore Authentication authentication) throws Exception {
        return new ResponseEntity<>(eventService.setFavourite(id, value, authentication), HttpStatus.OK);
    }

    @ApiOperation("Check if a Service is favourited by the authenticated user.")
    @RequestMapping(path = "favourite/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getFavourite(@PathVariable String id, @ApiIgnore Authentication authentication) {
        List<Event> events;
        try {
            events = eventService.getEvents(Event.UserActionType.FAVOURITE.getKey(), id, authentication);
            if (!events.isEmpty()) {
                return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.info(e + "\nReturning favourite = 0");
        }
        return new ResponseEntity<>("0", HttpStatus.OK);
    }

    @ApiIgnore
    @ApiOperation("Retrieve all the favourited events.")
    @RequestMapping(path = "favourites/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getFavourites() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.FAVOURITE.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the favourited events of the authenticated user.")
    @RequestMapping(path = "favourites", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserFavourites(Authentication authentication) {
        return new ResponseEntity<>(eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), authentication), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the favourited events of a infraService with the specified ID.")
    @RequestMapping(path = "favourites/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceFavourites(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }
    // <-------- FAVORITES


    // RATINGS ---------->
    @ApiOperation("Set a rating to a Service from the authenticated user.")
    @RequestMapping(path = "rating/service/{id}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Event> setUserRating(@PathVariable String id, @RequestParam("rating") String rating, @ApiIgnore Authentication authentication) throws Exception {
        return new ResponseEntity<>(eventService.setRating(id, rating, authentication), HttpStatus.OK);
    }

    @ApiOperation("Get the rating of the authenticated user.")
    @RequestMapping(path = "rating/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getRating(@PathVariable String id, @ApiIgnore Authentication authentication) {
        List<Event> events;
        try {
            events = eventService.getEvents(Event.UserActionType.RATING.getKey(), id, authentication);
            if (events != null && !events.isEmpty()) {
                return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.info(e + "\nReturning rate = null");
        }
        return new ResponseEntity<>("null", HttpStatus.OK);
    }

    @ApiIgnore
    @ApiOperation("Retrieve all rating events.")
    @RequestMapping(path = "ratings/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getRatings() {
        return new ResponseEntity<>(eventService.getEvents(Event.UserActionType.RATING.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the rating events of the authenticated user.")
    @RequestMapping(path = "ratings", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserRating(Authentication authentication) {
        try {
            return new ResponseEntity<>(eventService.getUserEvents(Event.UserActionType.RATING.getKey(), authentication), HttpStatus.OK);
        } catch (Exception e) {
            logger.info(e + "\nReturning ratings = null");
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the rating events of a infraService with the specified ID.")
    @RequestMapping(path = "ratings/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceRatings(@PathVariable String id) {
        return new ResponseEntity<>(eventService.getServiceEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }
    // <---------- RATINGS

}