package eu.einfracentral.domain.aai;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description"})
@XmlAccessorType(XmlAccessType.FIELD)

public class Grant {
    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
