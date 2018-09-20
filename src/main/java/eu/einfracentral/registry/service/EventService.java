package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Event;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface EventService extends ResourceService<Event, Authentication> {

    /**
     * Toggle a favourited event true/false.
     * @param serviceId
     * @param userId
     * @return
     */
    Event toggleFavourite(String serviceId, String userId);

    /**
     * Set a rating on a service from the given user.
     * @param serviceId
     * @param userId
     * @param value
     * @return
     */
    Event setRating(String serviceId, String userId, String value) throws Exception;

    /**
     * Get all events of a specific type.
     * @param eventType
     * @return
     */
    List<Event> getEvents(String eventType);

    /**
     * Get events of a specific type, created by a user for a given service.
     * @param eventType
     * @param serviceId
     * @param userId
     * @return
     */
    List<Event> getEvents(String eventType, String serviceId, String userId);

    /**
     * Get all events of a specific type about the given service.
     * @param eventType
     * @param serviceId
     * @return
     */
    List<Event> getServiceEvents(String eventType, String serviceId);

    /**
     * Get all events of a specific type created by the user.
     * @param eventType
     * @param userId
     * @return
     */
    List<Event> getUserEvents(String eventType, String userId);

}
