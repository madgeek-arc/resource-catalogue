package eu.einfracentral.domain.vocabsOrSomethingThatDoesntCauseAFight;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by pgl on 02/07/17.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description", "members"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Region {
    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = true, nillable = false)
    private String description;

    @XmlElementWrapper(required = true, nillable = false)
    @XmlElement(name = "member")
    private List<Country> members;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        throw new Error("No.");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Country> getMembers() {
        return members;
    }

    public void setMembers(List<Country> members) {
        this.members = members;
    }
}
