package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Event;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface EventService extends ResourceService<Event, Authentication> {

    /**
     * Toggle a favourited event true/false.
     *
     * @param serviceId
     * @param authentication
     * @return
     */
    Event setFavourite(String serviceId, Boolean value, Authentication authentication) throws Exception;

    /**
     * Set a rating on a service from the given user.
     *
     * @param serviceId
     * @param authentication
     * @param value
     * @return
     */
    Event setRating(String serviceId, String value, Authentication authentication) throws Exception;

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

    void deleteEvents(List<Event> events);
}
