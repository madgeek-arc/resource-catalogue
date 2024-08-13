package gr.uoa.di.madgik.resourcecatalogue.service;

import com.google.i18n.phonenumbers.NumberParseException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface EventService extends ResourceService<Event> {

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

    /**
     * @param events
     */
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

    /**
     * Set a Visit Event on a Service.
     *
     * @param serviceId
     * @param value
     * @return
     */
    Event setVisit(String serviceId, Float value) throws ResourceNotFoundException, NumberParseException;

    /**
     * Set an AddToProject Event on a Service.
     *
     * @param serviceId
     * @param value
     * @return
     */
    Event setAddToProject(String serviceId, Float value) throws ResourceNotFoundException, NumberParseException;

    /**
     * Set an Order Event on a Service.
     *
     * @param serviceId
     * @param value
     * @return
     */
    Event setOrder(String serviceId, Float value) throws ResourceNotFoundException, NumberParseException;

    /**
     * Set a scheduled Rating on a Service.
     *
     * @param serviceId
     * @param value
     * @return
     */
    Event setScheduledRating(String serviceId, Float value) throws ResourceNotFoundException, NumberParseException;

    /**
     * Get an aggregate of Visits on a specific Service.
     *
     * @param serviceId
     * @return
     */
    int getServiceAggregatedVisits(String serviceId);

    /**
     * Get an aggregate of AddToProject on a specific Service.
     *
     * @param serviceId
     * @return
     */
    int getServiceAggregatedAddToProject(String serviceId);

    /**
     * Get an aggregate of Orders on a specific Service.
     *
     * @param serviceId
     * @return
     */
    int getServiceAggregatedOrders(String serviceId);
}