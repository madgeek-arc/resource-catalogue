package gr.uoa.di.madgik.resourcecatalogue.service.sync;

import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatasourceSync extends AbstractSyncService<Datasource> {

    public DatasourceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable:false}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/datasource";
    }
}
