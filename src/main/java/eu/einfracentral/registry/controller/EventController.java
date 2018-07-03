package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ServiceService;
import eu.einfracentral.registry.service.UserService;
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
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
@RequestMapping("event")
public class EventController extends ResourceController<Event> {

    @Autowired
    EventController(EventService service) {
        super(service);
    }

    @Autowired
    private SearchService searchService;

    @Autowired
    private ParserService parserService;

    @Autowired
    private UserService userService;

    @Autowired
    private InfraServiceService infraServiceService;

    Logger logger = Logger.getLogger(EventController.class);

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
    public ResponseEntity<List<Event>> getFavourites() throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(getEvents(Event.UserActionType.FAVOURITE.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the favourited events of a user with the specified ID.")
    @RequestMapping(path = "favourite/all/user/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserFavourites(@PathVariable String id) throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(getUserEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }

//    @ApiIgnore
    @ApiOperation("Retrieve all the favourited events of a infraService with the specified ID.")
    @RequestMapping(path = "favourite/all/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceFavourites(@PathVariable String id) throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), id), HttpStatus.OK);
    }


    @ApiOperation("Set a service as favorite for a user.")
    @RequestMapping(path = "favorite/service/{sId}/user/{uId}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> setFavourite(@PathVariable String sId, @PathVariable String uId) throws ExecutionException, InterruptedException, ResourceNotFoundException {
        // TODO: check if user and service exists ?
        return new ResponseEntity<>(toggleFavourite(sId, uId), HttpStatus.OK);
    }


    @ApiOperation("Check if a service is favourited by a user.")
    @RequestMapping(path = "favourite/service/{sId}/user/{uId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getFavourite(@PathVariable String sId, @PathVariable String uId) throws ExecutionException, InterruptedException {
        List<Event> events = getEvents(Event.UserActionType.FAVOURITE.getKey(), sId, uId);
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
    public ResponseEntity<List<Event>> getRatings() throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(getEvents(Event.UserActionType.RATING.getKey()), HttpStatus.OK);
    }

    @ApiOperation("Retrieve all the rating events of a user with the specified ID.")
    @RequestMapping(path = "rating/all/user/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getUserRating(@PathVariable String id) throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(getUserEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }

    //    @ApiIgnore
    @ApiOperation("Retrieve all the rating events of a infraService with the specified ID.")
    @RequestMapping(path = "rating/all/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Event>> getServiceRatings(@PathVariable String id) throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(getServiceEvents(Event.UserActionType.RATING.getKey(), id), HttpStatus.OK);
    }

    @ApiOperation("Set a service as favorite for a user.")
    @RequestMapping(path = "rating/service/{sId}/user/{uId}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Event> setUserRating(@PathVariable String sId, @PathVariable String uId, @RequestParam("rating") String rating)
            throws ExecutionException, InterruptedException, ResourceNotFoundException {
        // TODO: check if user and service exists ?
        return new ResponseEntity<>(setRating(sId, uId, rating), HttpStatus.OK);
    }


    @ApiOperation("Get the rating of a user for a specific service.")
    @RequestMapping(path = "rating/service/{sId}/user/{uId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> getRating(@PathVariable String sId, @PathVariable String uId) throws ExecutionException, InterruptedException {
        List<Event> events = getEvents(Event.UserActionType.RATING.getKey(), sId, uId);
        if (events.size() > 0) {
            return new ResponseEntity<>(events.get(0).getValue(), HttpStatus.OK);
        }
        return new ResponseEntity<>("0", HttpStatus.NOT_FOUND);
    }

    // <---------- RATINGS


    private Event toggleFavourite(String serviceId, String userId) throws ExecutionException, InterruptedException, ResourceNotFoundException {
        List<Event> events = getEvents(Event.UserActionType.FAVOURITE.getKey(), serviceId, userId);
        Event event;
        if (events.size() == 0) {
            event = new Event();
            event.setService(serviceId);
            event.setUser(userId);
            event.setType(Event.UserActionType.FAVOURITE.getKey());
            event.setValue("true");
            event = service.add(event);
        }
        else {
            event = events.get(0);
            event = booleanToggleValue(event);
            event = service.update(event);
        }
        return event;
    }


    private Event setRating(String serviceId, String userId, String value) throws ExecutionException, InterruptedException, ResourceNotFoundException {
        List<Event> events = getEvents(Event.UserActionType.RATING.getKey(), serviceId, userId);
        Event event;
        if (events.size() == 0) {
            event = new Event();
            event.setService(serviceId);
            event.setUser(userId);
            event.setType(Event.UserActionType.RATING.getKey());
            event.setValue(value);
            event = service.add(event);
        }
        else {
            event = events.get(0);
            event.setValue(value);
            event = service.update(event);
        }
        return event;
    }

    private List<Event> getEvents(String eventType) throws ExecutionException, InterruptedException {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType, "event");
        return pagingToList(event_resources);
    }

    private List<Event> getEvents(String eventType, String serviceId, String userId) throws ExecutionException, InterruptedException {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType + " AND service=" + serviceId + " AND event_user=" + userId, "event");
        return pagingToList(event_resources);
    }

    private List<Event> getServiceEvents(String eventType, String serviceId) throws ExecutionException, InterruptedException {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType + " AND service=" + serviceId, "event");
        return pagingToList(event_resources);
    }

    private List<Event> getUserEvents(String eventType, String userId) throws ExecutionException, InterruptedException {
        Paging<Resource> event_resources = searchService
                .cqlQuery("type=" + eventType+ " AND event_user=" + userId, "event");
        return pagingToList(event_resources);
    }

    private Event booleanToggleValue(Event event) {
        if ("true".equals(event.getValue())) {
            event.setValue("false");
        } else {
            event.setValue("true");
        }
        return event;
    }

    private List<Event> pagingToList(Paging<Resource> resources) {
        List<Event> events = resources.getResults().stream().map(resource -> {
            try {
                return parserService.deserialize(resource, Event.class).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        logger.info(events.toString());
        return events;
    }
}

