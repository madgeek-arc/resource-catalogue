package eu.einfracentral.service.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataSourceSync extends AbstractSyncService<eu.einfracentral.domain.DataSource>{

    @Autowired
    public DataSourceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/dataSource";
    }
}
