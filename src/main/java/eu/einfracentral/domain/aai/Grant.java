package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Identifiable;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Grant implements Identifiable {
    @XmlElement(required = true)
    private String id;

    @XmlElement(required = true)
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
