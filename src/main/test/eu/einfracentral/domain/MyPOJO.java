package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 23/08/17.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "field"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MyPOJO implements Identifiable {
    @XmlElement(required = false)
    private String id;
    @XmlElement(required = false)
    private String field;

    public MyPOJO() {

    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}