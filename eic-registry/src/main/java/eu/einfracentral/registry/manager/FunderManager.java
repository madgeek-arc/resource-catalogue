package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.registry.service.FunderService;
import org.springframework.stereotype.Component;

@Component
public class FunderManager extends ResourceManager<Funder> implements FunderService {
    public FunderManager() {
        super(Funder.class);
    }

    @Override
    public String getResourceType() {
        return "funder";
    }
}
