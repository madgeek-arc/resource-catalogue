package eu.einfracentral.domain;

import eu.einfracentral.domain.aai.Grant;
import eu.einfracentral.domain.aai.User;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id"})
@XmlAccessorType(XmlAccessType.FIELD)

public class Provider {
    @XmlElement(required = true)
    private int id;

}
