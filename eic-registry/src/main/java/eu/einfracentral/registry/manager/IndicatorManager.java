package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.IndicatorService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        validate(indicator);
        super.add(indicator, auth);
        return indicator;
    }

    @Override
    public Indicator get(String id, Authentication auth) {
        return null;
    }

    @Override
    public void delete(Indicator indicator) {
        super.delete(indicator);
    }


    @Override
    public Indicator validate(Indicator indicator){

        //TODO: Add validation for existing Indicator ID
        // Validates Indicator's ID
        if (indicator.getId() == null || indicator.getId().equals("")) {
            throw new ValidationException("Indicator's id cannot be 'null' or 'empty'");
        }

        // Validates Indicator's description
        if (indicator.getDescription() == null || indicator.getDescription().equals("")) {
            throw new ValidationException("Indicator's description cannot be 'null' or 'empty'");
        }

        //Validates Indicator's dimensions
        if (indicator.getDimensions() == null || indicator.getDimensions().isEmpty()) {
            throw new ValidationException("Indicator's dimensions cannot be 'null' or 'empty'");
        }

        List<Indicator.DimensionType> validatedDimensions = new ArrayList<>();
        for (int i=0; i<indicator.getDimensions().size(); i++){
            if (indicator.getDimensions().get(i) == Indicator.DimensionType.TIME && !validatedDimensions.contains(Indicator.DimensionType.TIME)){
                validatedDimensions.add(indicator.getDimensions().get(i));
            }
            if (indicator.getDimensions().get(i) == Indicator.DimensionType.LOCATION && !validatedDimensions.contains(Indicator.DimensionType.LOCATION)){
                validatedDimensions.add(indicator.getDimensions().get(i));
            }
        }
        indicator.setDimensions(validatedDimensions);

        //Validates Indicator's unit
        if (indicator.getUnit() == null) {
            throw new ValidationException("Indicator's dimensions cannot be 'null' or 'empty'");
        }

        return indicator;
    }


}