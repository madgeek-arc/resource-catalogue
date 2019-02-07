package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.IndicatorService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class IndicatorManager extends ResourceManager<Indicator> implements IndicatorService<Indicator, Authentication> {

    //@Autowired
    public IndicatorManager() {
        super(Indicator.class);
    }

    @Override
    public String getResourceType() {
        return "indicator";
    }


    @Override
    public Indicator add(Indicator indicator, Authentication auth) {
        Indicator ret;

        //TODO: Create a validation method
        //Indicator's Validation
        if (indicator.getId() == null || indicator.getId().equals("")) {
            throw new ValidationException("Indicator's id cannot be 'null' or 'empty'");
        }
        if (indicator.getDescription() == null || indicator.getDescription().equals("")) {
            throw new ValidationException("Indicator's description cannot be 'null' or 'empty'");
        }

//        super.add(indicator, auth);
//        return indicator;
        ret = super.add(indicator, null);
        return ret;

    }

    @Override
    public Indicator get(String id, Authentication auth) {
        return null;

    }

    @Override
    public void delete(Indicator indicator) {
        super.delete(indicator);
    }

}