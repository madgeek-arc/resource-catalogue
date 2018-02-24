package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Manager;
import eu.einfracentral.registry.service.ManagerService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 08/01/18.
 */
@Service("managerService")
public class ManagerServiceImpl extends ResourceServiceImpl<Manager> implements ManagerService {
    public ManagerServiceImpl() {
        super(Manager.class);
    }

    @Override
    public String getResourceType() {
        return "manager";
    }
}
