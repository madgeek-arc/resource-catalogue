package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Measurement;
import eu.einfracentral.registry.service.MeasurementService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 24/04/18.
 */
@Service("measurementService")
public class MeasurementManager extends ResourceManager<Measurement> implements MeasurementService {
    public MeasurementManager() {
        super(Measurement.class);
    }

    @Override
    public String getResourceType() {
        return "measurement";
    }
}