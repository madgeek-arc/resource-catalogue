package eu.einfracentral.domain.vocabsOrSomethingThatDoesntCauseAFight;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 29/6/2017.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "val"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Category {

    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = true, nillable = false)
    private String val;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        throw new Error("No.");
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    //vals: Storage, Computing, Networking, Data, DataManagement, Identification, Consultancy;
}