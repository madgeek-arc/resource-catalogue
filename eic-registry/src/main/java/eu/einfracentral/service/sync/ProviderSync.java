package eu.einfracentral.service.sync;

import eu.einfracentral.domain.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProviderSync extends AbstractSyncService<Provider> {

    @Autowired
    public ProviderSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename) {
        super(host, filename);
    }

    @Override
    protected String getController() {
        return "/provider";
    }
}
