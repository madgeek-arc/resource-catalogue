package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Indicator implements Identifiable {
    @ApiModelProperty(position = 1, example = "Indicator's ID")
    @XmlElement(required = true)
    private String id;

    @ApiModelProperty(position = 2, example = "Indicator's name")
    @XmlElement(required = true)
    private String name;

    @ApiModelProperty(position = 3, example = "Indicator's short description")
    @XmlElement(required = true)
    private String description;

    @ApiModelProperty(position = 4, example = "['time', 'locations'] (at least one)")
    @XmlElementWrapper(name = "dimensions")
    @XmlElement(name = "dimension", required = true)
    private List<String> dimensions;

    @ApiModelProperty(position = 5, example = "'percentage', 'numeric' or 'boolean'")
    @XmlElement(required = true)
    private String unit;

    @ApiModelProperty(position = 6, example= "'days', 'km', etc")
    @XmlElement(required = true)
    private String unitName;

    public Indicator() {

    }

    public Indicator(Indicator indicator) {
        this.id = indicator.getId();
        this.name = indicator.getName();
        this.description = indicator.getDescription();
        this.unit = indicator.getUnit();
        this.dimensions = indicator.getDimensions();
        this.unitName = indicator.getUnitName();
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
                    .orElseThrow(() -> new IllegalArgumentException("Unknown value: " + s + " ; Valid options: "
                                    + Arrays.stream(values())
                                    .map(UnitType::getKey)
                                    .collect(Collectors.joining(", "))));
        }

    }

    public enum DimensionType {
        TIME("time"),
        LOCATIONS("locations");

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
                    .orElseThrow(() -> new IllegalArgumentException("Unknown value: " + s + " ; Valid options: "
                            + Arrays.stream(values())
                            .map(DimensionType::getKey)
                            .collect(Collectors.joining(", "))));
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }
}
