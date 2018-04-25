package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Dimension {
    public enum DimensionType {
        TIME,
        LOCATION;

        DimensionType() {
        }
    }
}
