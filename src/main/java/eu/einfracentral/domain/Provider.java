package eu.einfracentral.domain;

import eu.einfracentral.domain.aai.User;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id"})
@XmlAccessorType(XmlAccessType.FIELD)

public class Provider {
    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private String contactInformation;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "user")
    private List<User> users;
}
