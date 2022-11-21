package eu.einfracentral.domain.interoperabilityRecordInternalFields;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Right {

    /**
     * The URI of the license.
     */
    @XmlElement
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
    private URL rightURI;

    /**
     * A short, standardized version of the license name.
     */
    @XmlElement
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation
    private String rightIdentifier;

    public Right() {
    }

    public Right(String right, URL rightURI, String rightIdentifier) {
        this.rightURI = rightURI;
        this.rightIdentifier = rightIdentifier;
    }

    @Override
    public String toString() {
        return "RightsInfo{" +
                ", rightURI=" + rightURI +
                ", rightIdentifier='" + rightIdentifier + '\'' +
                '}';
    }

    public URL getRightURI() {
        return rightURI;
    }

    public void setRightURI(URL rightURI) {
        this.rightURI = rightURI;
    }

    public String getRightIdentifier() {
        return rightIdentifier;
    }

    public void setRightIdentifier(String rightIdentifier) {
        this.rightIdentifier = rightIdentifier;
    }
}
