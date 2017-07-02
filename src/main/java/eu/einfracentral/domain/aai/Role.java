package eu.einfracentral.domain.aai;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Role {
    @XmlElement(required = true, nillable = false)
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        throw new Error("No.");
    }
}
