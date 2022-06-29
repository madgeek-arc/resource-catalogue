package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Identifier {

    @XmlElementWrapper(name = "alternativeIdentifiers")
    @XmlElement(name = "alternativeIdentifier")
    private List<String> alternativeIdentifiers;

    @XmlElement()
    private boolean hidden;

    @XmlElement()
    private String originalId;

    public Identifier() {
    }

    public Identifier(Identifier identifier) {
        this.alternativeIdentifiers = identifier.getAlternativeIdentifiers();
        this.hidden = identifier.isHidden();
        this.originalId = identifier.getOriginalId();
    }

    @Override
    public String toString() {
        return "Identifier{" +
                "alternativeIdentifiers=" + alternativeIdentifiers +
                ", hidden=" + hidden +
                ", originalId='" + originalId + '\'' +
                '}';
    }

    public static Identifier createIdentifier(String originalId){
        Identifier ret = new Identifier();
        ret.setOriginalId(originalId);
        return ret;
    }

    public List<String> getAlternativeIdentifiers() {
        return alternativeIdentifiers;
    }

    public void setAlternativeIdentifiers(List<String> alternativeIdentifiers) {
        this.alternativeIdentifiers = alternativeIdentifiers;
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
