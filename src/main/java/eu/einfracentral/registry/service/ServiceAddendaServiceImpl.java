package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ServiceAddenda;

@org.springframework.stereotype.Service("serviceAddendaService")
public class ServiceAddendaServiceImpl extends ResourceServiceImpl<ServiceAddenda> implements ServiceAddendaService {
    public ServiceAddendaServiceImpl() {
        super(ServiceAddenda.class);
    }

    @Override
    public String getResourceType() {
        return "serviceAddenda";
    }
}
