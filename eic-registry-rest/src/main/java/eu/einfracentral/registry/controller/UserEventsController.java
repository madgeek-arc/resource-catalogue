package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("userEvents")
public class UserEventsController {

    private EventService eventService;
    private InfraServiceService<InfraService, InfraService> infraServiceService;


    @Autowired
    UserEventsController(EventService eventService, InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.eventService = eventService;
        this.infraServiceService = infraServiceService;
    }

    @ApiOperation("Retrieve all the favourite services of the authenticated user.")
    @RequestMapping(path = "favourites", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<RichService>> favourites(Authentication auth) {

        Map<String, String> favouriteServices = new HashMap<>();
        List<Event> userEvents = eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), auth);
        List<RichService> services = new ArrayList<>();
        for (Event userEvent : userEvents) {
            favouriteServices.putIfAbsent(userEvent.getService(), userEvent.getValue());
        }
        for (Map.Entry<String, String> favouriteService : favouriteServices.entrySet()) {
            if ("1".equals(favouriteService.getValue())) { // "1" is true
//                services.add(infraServiceService.createRichService(infraServiceService.get(favouriteService.getKey()), auth)); // TODO remove
                services.add(infraServiceService.getRichService(favouriteService.getKey(), "latest", auth));
            }
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }


    @ApiOperation("Retrieve all the rated services of the authenticated user.")
    @RequestMapping(path = "ratings", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, RichService>> ratings(Authentication auth) {

        List<Event> userEvents = eventService.getUserEvents(Event.UserActionType.RATING.getKey(), auth);
        Map<String, Float> serviceRatings = new HashMap<>();

        for (Event userEvent : userEvents) {
            serviceRatings.putIfAbsent(userEvent.getService(), Float.parseFloat(userEvent.getValue()));
        }
        Map<String, RichService> services = new HashMap<>();
        for (Map.Entry<String, Float> entry : serviceRatings.entrySet()) {
//            RichService richService = infraServiceService.createRichService(infraServiceService.get(entry.getKey()), auth); // TODO remove
            RichService richService = infraServiceService.getRichService(entry.getKey(), "latest", auth);
            richService.setHasRate(entry.getValue());
            services.put(entry.getKey(), richService);
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

}
