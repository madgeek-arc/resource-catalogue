package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Indicator;
import org.springframework.stereotype.Service;

@Service("indicatorService")
public interface IndicatorService extends ResourceService<Indicator> {
}
