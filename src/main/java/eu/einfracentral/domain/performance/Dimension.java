package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 06/11/17.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {
})
@XmlAccessorType(XmlAccessType.FIELD)
public class Dimension {
    public enum DimensionTypes {
        TIME,
        LOCATION;

        DimensionTypes() {
        }
    }
}
