package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.ServiceAddenda;
import eu.einfracentral.registry.service.ServiceAddendaService;
import org.springframework.stereotype.Service;

@Service("serviceAddendaService")
public class ServiceAddendaManager extends ResourceManager<ServiceAddenda> implements ServiceAddendaService {
    public ServiceAddendaManager() {
        super(ServiceAddenda.class);
    }

    @Override
    public String getResourceType() {
        return "serviceAddenda";
    }
}
