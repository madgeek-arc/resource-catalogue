package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.registry.service.IndicatorService;
import org.springframework.stereotype.Component;

@Component
public class IndicatorManager extends ResourceManager<Indicator> implements IndicatorService {
    public IndicatorManager() {
        super(Indicator.class);
    }

    @Override
    public String getResourceType() {
        return "indicator";
    }
}