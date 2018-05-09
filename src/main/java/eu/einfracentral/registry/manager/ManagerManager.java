package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Manager;
import eu.einfracentral.registry.service.ManagerService;
import org.springframework.stereotype.Component;

@Component
public class ManagerManager extends ResourceManager<Manager> implements ManagerService {
    public ManagerManager() {
        super(Manager.class);
    }

    @Override
    public String getResourceType() {
        return "manager";
    }
}
