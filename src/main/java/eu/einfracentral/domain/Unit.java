package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 06/11/17.
 */
@XmlType
public class Unit {
    public enum UnitTypes {
        PCT,
        NUM,
        BOOL;

        UnitTypes() {
        }
    }
}
