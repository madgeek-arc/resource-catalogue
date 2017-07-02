package eu.einfracentral.domain.vocabsOrSomethingThatDoesntCauseAFight;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 02/07/17.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "code"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Language {
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
     * ISO 639-3:2007 (SIL) code
     */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * ISO 639-3:2007 (SIL) name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
