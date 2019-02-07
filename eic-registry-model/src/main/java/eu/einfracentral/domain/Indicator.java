package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import javax.xml.bind.ValidationException;
import javax.xml.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Indicator implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String description;
    @XmlElementWrapper(name = "units")
    @XmlElement(name = "unit")
    @ApiModelProperty(required = true)
    private List<String> units;
    @XmlElementWrapper(name = "dimensions")
    @XmlElement(name = "dimension")
    @ApiModelProperty(required = true)
    private List<String> dimensions;

    public Indicator() {

    }

    public Indicator(Indicator indicator) {
        this.id = indicator.getId();
        this.description = indicator.getDescription();
        this.units = indicator.getUnits();
        this.dimensions = indicator.getDimensions();
    }


    public enum UnitType {
        PCT("percentage"),
        NUM("numeric"),
        BOOL("boolean");

        private final String unitType;

        UnitType(final String unitType) {
            this.unitType = unitType;
        }

        public String getKey() {
            return unitType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static UnitType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(UnitType.values())
                    .filter(v -> v.unitType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

    }

    public enum DimensionType {
        TIME("time"),
        LOCATION("location");

        private final String dimensionType;

        DimensionType(final String dimensionType) {
            this.dimensionType = dimensionType;
        }

        public String getKey() {
            return dimensionType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static DimensionType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(DimensionType.values())
                    .filter(v -> v.dimensionType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getUnits() {
        return units;
    }

    public void setUnits(List<String> units) {

        //Indicator's Validation
        //TODO: ValidationException is never thrown - FIX
        for (String aUnit : units) {
            if (!aUnit.equals("numeric") && !aUnit.equals("percentage") && !aUnit.equals("boolean")) {
                try {
                    throw new ValidationException("Valid unit types include 'percentage', 'numeric' and 'boolean'");
                } catch (ValidationException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        this.units = units;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {

        //Dimensions's Validation
        //TODO: ValidationException is never thrown - FIX
        for (String aDimension : dimensions) {
            if (!aDimension.equals("time") && !aDimension.equals("location")) {
                try {
                    throw new ValidationException("Valid unit types include 'time' and 'location'");
                } catch (ValidationException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        this.dimensions = dimensions;
    }
}
