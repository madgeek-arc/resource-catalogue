package gr.uoa.di.madgik.resourcecatalogue.service.sync;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceSync extends AbstractSyncService<gr.uoa.di.madgik.resourcecatalogue.domain.Service> {

    public ServiceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable:false}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/service";
    }

}
