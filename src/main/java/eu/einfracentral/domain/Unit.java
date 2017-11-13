package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 06/11/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Unit {
    public enum UnitTypes {
        PCT,
        NUM,
        BOOL;

        UnitTypes() {
        }
    }
}
