package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Measurement;
import org.springframework.security.core.Authentication;

public interface MeasurementService extends ResourceService<Measurement, Authentication> {
}
