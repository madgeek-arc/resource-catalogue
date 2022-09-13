package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.RichResource;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("userEvents")
public class UserEventsController {

    private final EventService eventService;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    UserEventsController(EventService eventService, ResourceBundleService<ServiceBundle> resourceBundleService) {
        this.eventService = eventService;
        this.resourceBundleService = resourceBundleService;
    }

    /**
     * Retrieve all the favourite Services of the authenticated user.
     *
     * @param auth
     * @return
     */
    @GetMapping(path = "favourites", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RichResource>> favourites(Authentication auth) {

            Map<String, Float> favouriteServices = new HashMap<>();
        List<Event> userEvents = eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), auth);
        List<RichResource> services = new ArrayList<>();

        // Check if the serviceId exists and add it on the list, so to avoid errors
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<String> serviceIds = new ArrayList<>();
        for (ServiceBundle serviceBundle : resourceBundleService.getAll(ff, auth).getResults()) {
            serviceIds.add(serviceBundle.getService().getId());
        }

        for (Event userEvent : userEvents) {
            if (serviceIds.contains(userEvent.getService())) {
                favouriteServices.putIfAbsent(userEvent.getService(), userEvent.getValue());
            }
        }
        for (Map.Entry<String, Float> favouriteService : favouriteServices.entrySet()) {
            if (favouriteService.getValue() == 1) { // "1" is true
                services.add(resourceBundleService.getRichResource(favouriteService.getKey(), catalogueName, auth));
            }
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    /**
     * Retrieve all the rated Services of the authenticated user.
     *
     * @param auth
     * @return
     */
    @GetMapping(path = "ratings", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RichResource>> ratings(Authentication auth) {

        Map<String, Float> serviceRatings = new HashMap<>();
        List<Event> userEvents = eventService.getUserEvents(Event.UserActionType.RATING.getKey(), auth);
        List<RichResource> services = new ArrayList<>();
        for (Event userEvent : userEvents) {
            serviceRatings.putIfAbsent(userEvent.getService(), userEvent.getValue());
        }
        for (Map.Entry<String, Float> serviceRating : serviceRatings.entrySet()) {
            services.add(resourceBundleService.getRichResource(serviceRating.getKey(), catalogueName, auth));
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

}
