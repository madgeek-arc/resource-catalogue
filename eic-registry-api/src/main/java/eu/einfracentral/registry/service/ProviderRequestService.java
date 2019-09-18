package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ProviderRequest;
import org.springframework.security.core.Authentication;

public interface ProviderRequestService<T, U extends Authentication> extends ResourceService<ProviderRequest, Authentication> {

}
