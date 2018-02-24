package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.ServiceAddenda;
import eu.einfracentral.registry.service.ServiceAddendaService;
import org.springframework.stereotype.Service;

@Service("serviceAddendaService")
public class ServiceAddendaServiceImpl extends ResourceServiceImpl<ServiceAddenda> implements ServiceAddendaService {
    public ServiceAddendaServiceImpl() {
        super(ServiceAddenda.class);
    }

    @Override
    public String getResourceType() {
        return "serviceAddenda";
    }
}
