package eu.einfracentral.domain;

import eu.einfracentral.domain.aai.Grant;
import eu.einfracentral.domain.aai.User;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"wtfdyjfsamylb?"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Provider {
    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElementWrapper(required = true, nillable = false)
    @XmlElement(name = "user")
    private Map<User, List<Grant>> users;
}
