package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.domain.Measurement;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.IndicatorService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndicatorManager extends ResourceManager<Indicator> implements IndicatorService<Indicator, Authentication> {

    private static final Logger logger = LogManager.getLogger(IndicatorManager.class);
    private MeasurementManager measurementManager;

    @Autowired
    public IndicatorManager(@Lazy MeasurementManager measurementManager) {
        super(Indicator.class);
        this.measurementManager = measurementManager;
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
    public Indicator update(Indicator indicator, Authentication auth) {
        validate(indicator);
        super.update(indicator, auth);
        return indicator;
    }

    @Override
    public Indicator get(String id, Authentication auth) {
        return null;
    }

    @Override
    public void delete(Indicator indicator) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("indicator", indicator.getId());
        Browsing<Measurement> measurements = measurementManager.getAll(ff, null);
        if (measurements.getTotal() > 0) {
            throw new ValidationException("You can't delete the specific Indicator, as it's related to one or more Measurements");
        }
        logger.info("Deleting indicator: " + indicator.getId());
        super.delete(indicator);
    }


    @Override
    public Indicator validate(Indicator indicator) {

        // Validates Indicator's ID
        if (indicator.getId() == null || indicator.getId().equals("")) {
            throw new ValidationException("Indicator's id cannot be 'null' or 'empty'");
        }

        // Validates Indicator's name
        if (indicator.getName() == null || indicator.getName().equals("")) {
            throw new ValidationException("Indicator's name cannot be 'null' or 'empty'");
        }

        // Validates Indicator's description
        if (indicator.getDescription() == null || indicator.getDescription().equals("")) {
            throw new ValidationException("Indicator's description cannot be 'null' or 'empty'");
        }

        // Validates Indicator's dimensions
        if (indicator.getDimensions() == null || indicator.getDimensions().isEmpty()) {
            throw new ValidationException("Indicator's dimensions cannot be 'null' or 'empty'");
        }

        List<String> validatedDimensions = new ArrayList<>();
        for (String dimension : indicator.getDimensions()) {
            if (Indicator.DimensionType.fromString(dimension) == Indicator.DimensionType.TIME
                    && !validatedDimensions.contains(Indicator.DimensionType.TIME.getKey())) {
                validatedDimensions.add(dimension);
            }
            if (Indicator.DimensionType.fromString(dimension) == Indicator.DimensionType.LOCATIONS
                    && !validatedDimensions.contains(Indicator.DimensionType.LOCATIONS.getKey())) {
                validatedDimensions.add(dimension);
            }
        }
        indicator.setDimensions(validatedDimensions);

        // Validates Indicator's unit
        if (indicator.getUnit() == null) {
            throw new ValidationException("Indicator's unit cannot be 'null' or 'empty'");
        }

        // throws exception if value does not exist
        Indicator.UnitType.fromString(indicator.getUnit());

        return indicator;
    }


}