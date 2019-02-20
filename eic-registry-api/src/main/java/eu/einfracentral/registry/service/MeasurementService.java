package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Measurement;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;

public interface MeasurementService<T, U extends Authentication> extends ResourceService<Measurement, Authentication> {

    /**
     * Paging with all measurements of the service with id {@param serviceId}
     * @param serviceId
     * @param authentication
     * @return
     */
    Paging<T> getAll(String serviceId, U authentication);

    /**
     *
     * @param serviceId
     * @param authentication
     * @return
     */
    Paging<T> getLatestServiceMeasurements(String serviceId, U authentication);

    /**
     * Paging with all measurements for the specified {@param indicatorId} and {@param serviceId}.
     *
     * @param indicatorId
     * @param serviceId
     * @param authentication
     * @return
     */
    Paging<T> getAll(String indicatorId, String serviceId, U authentication);

    /**
     * Searches if an identical measurement exists (except Id field)
     * @param measurement
     * @return
     */
    boolean existsIdentical(T measurement);

    /**
     * Ensures Measurement abides by Indicator's structure
     * @param measurement
     * @return
     */
    boolean validateMeasurementStructure(T measurement);

}
