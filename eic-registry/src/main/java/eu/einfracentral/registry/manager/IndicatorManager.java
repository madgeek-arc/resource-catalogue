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

@Deprecated
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
        logger.info("Adding Indicator: {}", indicator);
        return indicator;
    }

    @Override
    public Indicator update(Indicator indicator, Authentication auth) {
        validate(indicator);
        super.update(indicator, auth);
        logger.info("Updating Indicator: {}", indicator);
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
        logger.info("Deleting Indicator: {}", indicator);
        super.delete(indicator);
    }

    @Override
    public Indicator validate(Indicator indicator) {
        logger.debug("Validating Indicator: {}", indicator);

        super.validate(indicator);

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

        // Validates Indicator's unitName
        switch (Indicator.UnitType.fromString(indicator.getUnit())) {
            case NUM:
                if (indicator.getUnitName() == null || "".equals(indicator.getUnitName())) {
                    throw new ValidationException("Please specify a unitName.");
                }
                break;
            case PCT:
                if (indicator.getUnitName() != null && !"%".equals(indicator.getUnitName())) {
                    throw new ValidationException("unitName for 'percentage' unit must be set to '%'");
                } else {
                    indicator.setUnitName("%");
                }
                break;
            case BOOL:
                if (indicator.getUnitName() != null) {
                    throw new ValidationException("unitName for 'boolean' unit must be null");
                } else {
                    indicator.setUnitName("");
                }
                break;
        }

        // throws exception if value does not exist
        Indicator.UnitType.fromString(indicator.getUnit());

        return indicator;
    }

    boolean hasTime(Indicator indicator) {
        for (String dimension : indicator.getDimensions()) {
            if (dimension.equals("time")) {
                return true;
            }
        }
        return false;
    }

    boolean hasLocations(Indicator indicator) {
        for (String dimension : indicator.getDimensions()) {
            if (dimension.equals("locations")) {
                return true;
            }
        }
        return false;
    }

}
