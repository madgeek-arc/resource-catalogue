package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Manager;

/**
 * Created by pgl on 08/01/18.
 */
@org.springframework.stereotype.Service("managerService")
public class ManagerServiceImpl extends ResourceServiceImpl<Manager> implements ManagerService {
    public ManagerServiceImpl() {
        super(Manager.class);
    }

    @Override
    public String getResourceType() {
        return "manager";
    }
}
