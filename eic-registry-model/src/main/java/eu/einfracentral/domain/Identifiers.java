package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Identifiers {

    @XmlElement()
    private boolean hidden;

    @XmlElement()
    private String originalId;

    public Identifiers() {
    }

    public Identifiers(Identifiers identifiers) {
        this.hidden = identifiers.isHidden();
        this.originalId = identifiers.getOriginalId();
    }

    @Override
    public String toString() {
        return "Identifier{" +
                ", hidden=" + hidden +
                ", originalId='" + originalId + '\'' +
                '}';
    }

    public static Identifiers createIdentifier(String originalId){
        Identifiers ret = new Identifiers();
        ret.setOriginalId(originalId);
        return ret;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }
}
