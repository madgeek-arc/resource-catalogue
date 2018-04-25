package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

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
