package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("event")
public class EventController extends ResourceController<Event, Authentication> {

    private EventService service;

    @Autowired
    EventController(EventService service) {
        super(service);
        this.service = service;
    }

    private final Logger logger = Logger.getLogger(EventController.class);

    @ApiOperation("Retrieve all events.")
    @RequestMapping(path = "events/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Event>> getAll(Authentication authentication) {
        return new ResponseEntity<>(service.getAll(new FacetFilter(), authentication), HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve the event with a specific ID.")
    @RequestMapping(path = "id/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> get(@PathVariable String id, Authentication authentication) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    // FAVORITES -------->
//    @ApiIgnore
    @ApiOperation("Retrieve all the favourited events.")
    @RequestMapping(path = "favourites/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getFavourites() {
        return new ResponseEntity<>(service.getEvents(Event.UserActionType.FAVOURITE.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the favourited events of a user with the specified ID.")
    @RequestMapping(path = "favourites/my", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserFavourites(Authentication authentication) {
        try {
            return new ResponseEntity<>(service.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), authentication), HttpStatus.OK);
        } catch (Exception e) {
            logger.info(e + "\nReturning favourites=0");
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve all the favourited events of a infraService with the specified ID.")
    @RequestMapping(path = "favourites/all/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceFavourites(@PathVariable String id) {
        return new ResponseEntity<>(service.getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }


    @ApiOperation("Set a service as favorite for a user.")
    @RequestMapping(path = "favourites/service/{sId}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setFavourite(@PathVariable String sId, @RequestParam Integer value, Authentication authentication) throws Exception {
        // TODO: check if user and service exists ?
        return new ResponseEntity<>(service.setFavourite(sId, value, authentication), HttpStatus.OK);
    }


    @ApiOperation("Check if a service is favourited by the user.")
    @RequestMapping(path = "favourites/my/service/{sId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getFavourite(@PathVariable String sId, Authentication authentication) {
        List<Event> events;
        try {
            events = service.getEvents(Event.UserActionType.FAVOURITE.getKey(), sId, authentication);
            if (events.size() > 0) {
                return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.info(e + "\nReturning favourite = 0");
        }
        return new ResponseEntity<>("0", HttpStatus.OK);
    }


    // <-------- FAVORITES

    // RATINGS ---------->
//    @ApiIgnore
    @ApiOperation("Retrieve all rating events.")
    @RequestMapping(path = "ratings/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getRatings() {
        return new ResponseEntity<>(service.getEvents(Event.UserActionType.RATING.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the rating events of a user with the specified ID.")
    @RequestMapping(path = "ratings/my", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserRating(Authentication authentication) {
        try {
            return new ResponseEntity<>(service.getUserEvents(Event.UserActionType.RATING.getKey(), authentication), HttpStatus.OK);
        } catch (Exception e) {
            logger.info(e + "\nReturning ratings = null");
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve all the rating events of a infraService with the specified ID.")
    @RequestMapping(path = "ratings/all/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceRatings(@PathVariable String id) {
        return new ResponseEntity<>(service.getServiceEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }

    @ApiOperation("Set a rating to a service from the given user.")
    @RequestMapping(path = "ratings/my/service/{sId}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Event> setUserRating(@PathVariable String sId, @RequestParam("rating") String rating, Authentication authentication) throws Exception {
        // TODO: check if user and service exists ?
        return new ResponseEntity<>(service.setRating(sId, rating, authentication), HttpStatus.OK);
    }


    @ApiOperation("Get the rating of a user for a specific service.")
    @RequestMapping(path = "ratings/my/service/{sId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getRating(@PathVariable String sId, Authentication authentication) {
        List<Event> events;
        try {
            events = service.getEvents(Event.UserActionType.RATING.getKey(), sId, authentication);
            if (events != null && events.size() > 0) {
                return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.info(e + "\nReturning rate = null");
        }
        return new ResponseEntity<>("null", HttpStatus.OK);
    }

    // <---------- RATINGS

}