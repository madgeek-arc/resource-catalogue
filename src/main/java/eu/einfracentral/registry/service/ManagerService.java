package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Manager;
import org.springframework.stereotype.Service;

@Service("managerService")
public interface ManagerService extends ResourceService<Manager> {
}
