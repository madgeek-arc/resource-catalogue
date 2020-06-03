package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Indicator;
import org.springframework.security.core.Authentication;

@Deprecated
public interface IndicatorService<T, U extends Authentication> extends ResourceService<Indicator, Authentication> {

    T get(String id, U authentication);
}
