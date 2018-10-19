package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Measurement;
import eu.einfracentral.registry.service.MeasurementService;
import org.springframework.stereotype.Component;

@Component
public class MeasurementManager extends ResourceManager<Measurement> implements MeasurementService {
    public MeasurementManager() {
        super(Measurement.class);
    }

    @Override
    public String getResourceType() {
        return "measurement";
    }
}