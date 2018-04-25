package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Indicator;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 24/04/18.
 */
@Service("indicatorService")
public interface IndicatorService extends ResourceService<Indicator> {
}
