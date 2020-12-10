package eu.einfracentral.registry.service;

import com.google.i18n.phonenumbers.NumberParseException;
import eu.einfracentral.domain.Event;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface EventService extends ResourceService<Event, Authentication> {

    /**
     * Toggle a favourited event true/false.
     *
     * @param serviceId
     * @param authentication
     * @return
     */
    Event setFavourite(String serviceId, Float value, Authentication authentication) throws ResourceNotFoundException;

    /**
     * Set a rating on a service from the given user.
     *
     * @param serviceId
     * @param authentication
     * @param value
     * @return
     */
    Event setRating(String serviceId, Float value, Authentication authentication) throws ResourceNotFoundException, NumberParseException;

    /**
     * Get all events of a specific type.
     *
     * @param eventType
     * @return
     */
    List<Event> getEvents(String eventType);

    /**
     * Get events of a specific type, created by a user for a given service.
     *
     * @param eventType
     * @param serviceId
     * @param authentication
     * @return
     */
    List<Event> getEvents(String eventType, String serviceId, Authentication authentication);

    /**
     * Get all events of a specific type about the given service.
     *
     * @param eventType
     * @param serviceId
     * @return
     */
    List<Event> getServiceEvents(String eventType, String serviceId);

    /**
     * Get all events of a specific type created by the user.
     *
     * @param eventType
     * @param authentication
     * @return
     */
    List<Event> getUserEvents(String eventType, Authentication authentication);

    /**
     * Retrieve a map with service IDs as keys and list of float event values for each service.
     *
     * @param eventType
     * @param authentication
     * @return
     */
    Map<String, List<Float>> getAllServiceEventValues(String eventType, Authentication authentication);

    void deleteEvents(List<Event> events);

    /**
     * Add visits on a Service on a specific day.
     *
     * @param date
     * @param serviceId
     * @param noOfVisits
     * @param authentication
     */
    void addVisitsOnDay(Date date, String serviceId, Float noOfVisits, Authentication authentication);
}
