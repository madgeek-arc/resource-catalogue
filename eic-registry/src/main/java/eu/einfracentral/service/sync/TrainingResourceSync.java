package eu.einfracentral.service.sync;

import eu.einfracentral.domain.TrainingResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TrainingResourceSync extends AbstractSyncService<TrainingResource> {

    @Autowired
    public TrainingResourceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/trainingResource";
    }
}
