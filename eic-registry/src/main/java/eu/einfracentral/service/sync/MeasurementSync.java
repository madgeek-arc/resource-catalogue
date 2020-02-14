package eu.einfracentral.service.sync;

import eu.einfracentral.domain.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("measurementSync")
public class MeasurementSync extends AbstractSyncService<Measurement> {

    @Override
    protected String getController() {
        return "/measurement";
    }

    @Autowired
    public MeasurementSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename) {
        super(host, filename);
    }
}
