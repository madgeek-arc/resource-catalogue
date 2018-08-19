package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("event")
public class EventController extends ResourceController<Event> {

    private EventService service;

    @Autowired
    EventController(EventService service) {
        super(service);
        this.service = service;
    }

    @Autowired
    private SearchService searchService;

    private final Logger logger = Logger.getLogger(EventController.class);

    @ApiOperation("Retrieve all events.")
    @RequestMapping(path = "events/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<Event>> getAll() {
        return new ResponseEntity<>(service.getAll(new FacetFilter()), HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve the event with a specific ID.")
    @RequestMapping(path = "id/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> get(@PathVariable String id) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    // FAVORITES -------->
//    @ApiIgnore
    @ApiOperation("Retrieve all the favourited events.")
    @RequestMapping(path = "favourite/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getFavourites() {
        return new ResponseEntity<>(service.getEvents(Event.UserActionType.FAVOURITE.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the favourited events of a user with the specified ID.")
    @RequestMapping(path = "favourite/all/user/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserFavourites(@PathVariable String id) {
        return new ResponseEntity<>(service.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve all the favourited events of a infraService with the specified ID.")
    @RequestMapping(path = "favourite/all/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceFavourites(@PathVariable String id) {
        return new ResponseEntity<>(service.getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }


    @ApiOperation("Set a service as favorite for a user.")
    @RequestMapping(path = "favourite/service/{sId}/user/{uId}", method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> setFavourite(@PathVariable String sId, @PathVariable String uId) throws ResourceNotFoundException {
        // TODO: check if user and service exists ?
        return new ResponseEntity<>(service.toggleFavourite(sId, uId), HttpStatus.OK);
    }


    @ApiOperation("Check if a service is favourited by a user.")
    @RequestMapping(path = "favourite/service/{sId}/user/{uId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getFavourite(@PathVariable String sId, @PathVariable String uId) {
        List<Event> events = service.getEvents(Event.UserActionType.FAVOURITE.getKey(), sId, uId);
        if (events.size() > 0) {
            return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
        }
        return new ResponseEntity<>("false", HttpStatus.OK);
    }


    // <-------- FAVORITES

    // RATINGS ---------->
//    @ApiIgnore
    @ApiOperation("Retrieve all rating events.")
    @RequestMapping(path = "rating/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getRatings() {
        return new ResponseEntity<>(service.getEvents(Event.UserActionType.RATING.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the rating events of a user with the specified ID.")
    @RequestMapping(path = "rating/all/user/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserRating(@PathVariable String id) {
        return new ResponseEntity<>(service.getUserEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve all the rating events of a infraService with the specified ID.")
    @RequestMapping(path = "rating/all/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceRatings(@PathVariable String id) {
        return new ResponseEntity<>(service.getServiceEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }

    @ApiOperation("Set a rating to a service from the given user.")
    @RequestMapping(path = "rating/service/{sId}/user/{uId}", method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> setUserRating(@PathVariable String sId, @PathVariable String uId, @RequestParam("rating") String rating)
            throws ExecutionException, InterruptedException, ResourceNotFoundException {
        // TODO: check if user and service exists ?
        return new ResponseEntity<>(service.setRating(sId, uId, rating), HttpStatus.OK);
    }


    @ApiOperation("Get the rating of a user for a specific service.")
    @RequestMapping(path = "rating/service/{sId}/user/{uId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getRating(@PathVariable String sId, @PathVariable String uId) {
        List<Event> events = service.getEvents(Event.UserActionType.RATING.getKey(), sId, uId);
        if (events.size() > 0) {
            return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
        }
        return new ResponseEntity<>("0", HttpStatus.NOT_FOUND);
    }

    // <---------- RATINGS

}