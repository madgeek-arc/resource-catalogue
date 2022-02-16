package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class UseCasesPair {

    /**
     * Link to use cases supported by this Resource.
     */
    @XmlElement(name = "useCase", required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation()
    private URL useCaseURL;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElement(name = "useCasesName")
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String useCaseName;

    public UseCasesPair() {
    }

    public UseCasesPair(URL useCaseURL, String useCaseName) {
        this.useCaseURL = useCaseURL;
        this.useCaseName = useCaseName;
    }

    public URL getUseCaseURL() {
        return useCaseURL;
    }

    public void setUseCaseURL(URL useCaseURL) {
        this.useCaseURL = useCaseURL;
    }

    public String getUseCaseName() {
        return useCaseName;
    }

    public void setUseCaseName(String useCaseName) {
        this.useCaseName = useCaseName;
    }
}
