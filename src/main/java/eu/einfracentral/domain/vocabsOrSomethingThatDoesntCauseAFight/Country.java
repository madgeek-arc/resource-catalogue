package eu.einfracentral.domain.vocabsOrSomethingThatDoesntCauseAFight;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 02/07/17.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "code"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Country {
    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = false)
    private String code;

    @XmlElement(required = false)
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        throw new Error("No.");
    }

    /**
     * ISO 3166-1 alpha-2 code
     */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * ISO 3166-1 alpha-2 name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
