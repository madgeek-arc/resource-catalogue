package eu.einfracentral.domain.aai;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Grant {
    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = true, nillable = false)
    private String description;
}
