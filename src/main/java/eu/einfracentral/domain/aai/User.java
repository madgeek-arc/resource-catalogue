package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Provider;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class User {
    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = true, nillable = false)
    private Provider org;

    @XmlElement(required = true, nillable = false)
    private Role role;

    @XmlElementWrapper(required = true, nillable = false)
    @XmlElement(name = "grant")
    private List<Grant> grants;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        throw new Error("No.");
    }
}
