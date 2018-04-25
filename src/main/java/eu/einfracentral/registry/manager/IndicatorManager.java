package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.registry.service.IndicatorService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 24/04/18.
 */
@Service("indicatorService")
public class IndicatorManager extends ResourceManager<Indicator> implements IndicatorService {
    public IndicatorManager() {
        super(Indicator.class);
    }

    @Override
    public String getResourceType() {
        return "indicator";
    }
}