package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.SearchService;
import io.swagger.annotations.ApiOperation;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequestMapping("event")
public class EventController extends ResourceController<Event> {
    @Autowired
    EventController(EventService service) {
        super(service);
    }

    @Autowired
    private SearchService searchService;

    @ApiOperation("Retrieve all events.")
    @RequestMapping(path = "/get/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public List<Event> getAll() {
        return service.getAll(new FacetFilter()).getResults();
    }

    @ApiIgnore
    @ApiOperation("Retrieve the event with a specific ID.")
    @RequestMapping(path = "/get/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public Event get(@PathVariable String id) {

        return null;
    }

    @ApiIgnore
    @ApiOperation("Retrieve the favorited event with a specific ID.")
    @RequestMapping(path = "favorite/get/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public List<Event> getFavorite(@PathVariable String id) {

        return null;
    }

    @ApiOperation("Retrieve all the favorited events of a user with the specified ID.")
    @RequestMapping(path = "favorite/user/{id}/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public List<Event> getUserFavorites(@PathVariable String id) {
        service.get("type", Event.UserActionType.FAVOURITE.getKey());

        return null;
    }

    @ApiIgnore
    @ApiOperation("Retrieve all the favorited events of a infraService with the specified ID.")
    @RequestMapping(path = "favorite/service/{id}/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public List<Event> getServiceFavorites(@PathVariable String id) {
        searchService.cqlQuery("infraService="+id+" AND ","event",1000,0,"modification_date", SortOrder.DESC).getResults();
        return null;
    }
}

