package gr.uoa.di.madgik.service.sync;

import gr.uoa.di.madgik.domain.Datasource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatasourceSync extends AbstractSyncService<Datasource> {

    @Autowired
    public DatasourceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/datasource";
    }
}
