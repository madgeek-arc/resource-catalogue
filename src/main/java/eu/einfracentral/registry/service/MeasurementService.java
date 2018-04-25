package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import org.springframework.stereotype.Service;

@Service("measurementService")
public interface MeasurementService extends ResourceService<Measurement> {
}
