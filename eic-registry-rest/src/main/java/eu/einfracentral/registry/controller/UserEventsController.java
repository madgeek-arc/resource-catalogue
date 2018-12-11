package eu.einfracentral.registry.controller;
import eu.einfracentral.domain.Event;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("userEvent")
public class UserEventsController {

    private static final Logger logger = LogManager.getLogger(UserEventsController.class);
    private EventService eventService;
    private InfraServiceService<InfraService, InfraService> infraServiceService;


    @Autowired
    UserEventsController(EventService eventService, InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.eventService = eventService;
        this.infraServiceService = infraServiceService;
    }

    @ApiOperation("Retrieve all the favourite services of the authenticated user.")
    @RequestMapping(path = "favourites", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> favourites(Authentication auth) {

        Map<String, String> favouriteServices = new HashMap<>();
        List<Event> userEvents = eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), auth);
        List<Service> services = new ArrayList<>();
        Service service = null;
        for (Event userEvent : userEvents) {
            favouriteServices.putIfAbsent(userEvent.getService(), userEvent.getValue());
        }
        for (Map.Entry <String, String> favouriteService : favouriteServices.entrySet()) {
            if (favouriteService.getValue().equals("1")) {
                try {
                    services.add(infraServiceService.getLatest(favouriteService.getKey()));
                } catch (ResourceNotFoundException e) {
                    logger.warn("Could not find service: " + favouriteService.getKey());
                }
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
            try {
                RichService richService = infraServiceService.createRichService(infraServiceService.getLatest(entry.getKey()), auth);
                richService.setHasRate(entry.getValue());
                services.put(entry.getKey(), richService);
            } catch (ResourceNotFoundException e) {
                logger.warn("Could not find service: " + entry.getKey());
            }
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

}
