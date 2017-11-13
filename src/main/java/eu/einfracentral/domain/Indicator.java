package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 30/6/2017.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Indicator implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String description;
    @XmlElement(required = true)
    private Unit unit;
    @XmlElement(required = true)
    private Dimension[] dimensions;

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

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Dimension[] getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimension[] dimensions) {
        this.dimensions = dimensions;
    }
}
