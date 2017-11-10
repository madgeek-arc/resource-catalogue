package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 06/11/17.
 */
@XmlType
public class Dimension {
    public enum DimensionTypes {
        TIME,
        LOCATION;

        DimensionTypes() {
        }
    }
}
