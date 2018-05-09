package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Addenda;
import eu.einfracentral.registry.service.AddendaService;
import org.springframework.stereotype.Component;

@Component
public class AddendaManager extends ResourceManager<Addenda> implements AddendaService {
    public AddendaManager() {
        super(Addenda.class);
    }

    @Override
    public String getResourceType() {
        return "addenda";
    }
}
