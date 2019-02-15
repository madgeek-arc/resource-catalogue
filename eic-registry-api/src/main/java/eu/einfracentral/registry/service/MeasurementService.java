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
    Paging<T> getServiceMeasurements(String serviceId, U authentication);

    /**
     *
     * @param serviceId
     * @param authentication
     * @return
     */
    Paging<T> getLatestServiceMeasurements(String serviceId, U authentication);
}
