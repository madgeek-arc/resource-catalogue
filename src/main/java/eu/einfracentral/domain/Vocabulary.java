package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 3/7/2017.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "name", "type", "parent"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Vocabulary {

    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private String type;

    @XmlElement
    private String parent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

}
