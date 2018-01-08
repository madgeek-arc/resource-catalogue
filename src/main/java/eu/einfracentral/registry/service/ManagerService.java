package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Manager;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 08/01/18.
 */
@Service("managerService")
public interface ManagerService extends ResourceService<Manager> {
}
