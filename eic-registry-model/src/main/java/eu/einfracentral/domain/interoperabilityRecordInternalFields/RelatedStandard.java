package eu.einfracentral.domain.interoperabilityRecordInternalFields;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class RelatedStandard {

    /**
     * The name of the related standard.
     */
    @XmlElement
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private String relatedStandardIdentifier;

    /**
     * The URI of the related standard.
     */
    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private URL relatedStandardURI;

    public RelatedStandard() {
    }

    public RelatedStandard(String relatedStandardIdentifier, URL relatedStandardURI) {
        this.relatedStandardIdentifier = relatedStandardIdentifier;
        this.relatedStandardURI = relatedStandardURI;
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
