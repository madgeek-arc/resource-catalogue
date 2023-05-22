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
     * Right title.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
    private String rightTitle;

    /**
     * The URI of the license.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation
    private URL rightURI;

    /**
     * A short, standardized version of the license name.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation
    private String rightIdentifier;

    public Right() {
    }

    public Right(String rightTitle, URL rightURI, String rightIdentifier) {
        this.rightTitle = rightTitle;
        this.rightURI = rightURI;
        this.rightIdentifier = rightIdentifier;
    }

    @Override
    public String toString() {
        return "Right{" +
                "rightTitle='" + rightTitle + '\'' +
                ", rightURI=" + rightURI +
                ", rightIdentifier='" + rightIdentifier + '\'' +
                '}';
    }

    public String getRightTitle() {
        return rightTitle;
    }

    public void setRightTitle(String rightTitle) {
        this.rightTitle = rightTitle;
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
