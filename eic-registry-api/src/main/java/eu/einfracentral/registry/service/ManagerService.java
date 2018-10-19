package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Manager;
import org.springframework.security.core.Authentication;

public interface ManagerService extends ResourceService<Manager, Authentication> {
}
