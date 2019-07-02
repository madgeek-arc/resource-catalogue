package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ServiceOption;
import org.springframework.security.core.Authentication;

public interface ServiceOptionService <T, U extends Authentication> extends ResourceService<ServiceOption, Authentication> {
}
