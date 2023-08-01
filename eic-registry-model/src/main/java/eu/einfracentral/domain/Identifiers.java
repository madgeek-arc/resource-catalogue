package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Identifiers {

    @XmlElementWrapper(name = "alternativeIdentifiers")
    @XmlElement(name = "alternativeIdentifier")
    private List<AlternativeIdentifier> alternativeIdentifiers;

    @XmlElement()
    private String originalId;

    public Identifiers() {
    }

    public Identifiers(Identifiers identifiers) {
        this.alternativeIdentifiers = identifiers.getAlternativeIdentifiers();
        this.originalId = identifiers.getOriginalId();
    }

    @Override
    public String toString() {
        return "Identifier{" +
                "alternativeIdentifiers=" + alternativeIdentifiers +
                ", originalId='" + originalId + '\'' +
                '}';
    }

    public static void createOriginalId(Bundle<?> bundle){
        if (bundle.getIdentifiers() != null) {
            bundle.getIdentifiers().setOriginalId(bundle.getId());
        } else {
            Identifiers identifiers = new Identifiers();
            identifiers.setOriginalId(bundle.getId());
            bundle.setIdentifiers(identifiers);
        }
    }

    public List<AlternativeIdentifier> getAlternativeIdentifiers() {
        return alternativeIdentifiers;
    }

    public void setAlternativeIdentifiers(List<AlternativeIdentifier> alternativeIdentifiers) {
        this.alternativeIdentifiers = alternativeIdentifiers;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }
}
