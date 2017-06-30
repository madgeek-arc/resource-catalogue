package eu.einfracentral.domain.vocabsOrSomethingThatDoesntCauseAFight;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 29/6/2017.
 */
@XmlType(namespace = "http://einfracentral.eu")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public enum Phase {
    Beta, Production;

    public static Phase fromValue(String v) {
        return valueOf(v);
    }

    public String value() {
        return name();
    }
}