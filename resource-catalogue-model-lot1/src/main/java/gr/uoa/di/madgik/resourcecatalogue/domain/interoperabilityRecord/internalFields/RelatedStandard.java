package gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class RelatedStandard {

    /**
     * The name of the related standard.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String relatedStandardIdentifier;

    /**
     * The URI of the related standard.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URL relatedStandardURI;

    public RelatedStandard() {
    }

    public RelatedStandard(String relatedStandardIdentifier, URL relatedStandardURI) {
        this.relatedStandardIdentifier = relatedStandardIdentifier;
        this.relatedStandardURI = relatedStandardURI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelatedStandard that = (RelatedStandard) o;
        return Objects.equals(relatedStandardIdentifier, that.relatedStandardIdentifier) && Objects.equals(relatedStandardURI, that.relatedStandardURI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relatedStandardIdentifier, relatedStandardURI);
    }

    @Override
    public String toString() {
        return "RelatedStandard{" +
                "relatedStandardIdentifier='" + relatedStandardIdentifier + '\'' +
                ", relatedStandardURI=" + relatedStandardURI +
                '}';
    }

    public String getRelatedStandardIdentifier() {
        return relatedStandardIdentifier;
    }

    public void setRelatedStandardIdentifier(String relatedStandardIdentifier) {
        this.relatedStandardIdentifier = relatedStandardIdentifier;
    }

    public URL getRelatedStandardURI() {
        return relatedStandardURI;
    }

    public void setRelatedStandardURI(URL relatedStandardURI) {
        this.relatedStandardURI = relatedStandardURI;
    }
}
