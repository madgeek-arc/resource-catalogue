package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Measurement;
import org.springframework.security.core.Authentication;

public interface MeasurementService<T, U extends Authentication> extends ResourceService<Measurement, Authentication> {

    T get(String id, U authentication);
}
