package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 06/11/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Unit {
    public enum UnitType {
        PCT,
        NUM,
        BOOL;

        UnitType() {
        }
    }
}
