package gr.uoa.di.madgik.service.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceSync extends AbstractSyncService<gr.uoa.di.madgik.domain.Service> {

    @Autowired
    public ServiceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/service";
    }

}
