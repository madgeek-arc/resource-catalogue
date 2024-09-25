package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class UseCasesPair {

    /**
     * Link to use cases supported by this Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private URL useCaseURL;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true)
    private String useCaseName;

    public UseCasesPair() {
    }

    public UseCasesPair(URL useCaseURL, String useCaseName) {
        this.useCaseURL = useCaseURL;
        this.useCaseName = useCaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UseCasesPair that = (UseCasesPair) o;
        return Objects.equals(useCaseURL, that.useCaseURL) && Objects.equals(useCaseName, that.useCaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useCaseURL, useCaseName);
    }

    @Override
    public String toString() {
        return "UseCasesPair{" +
                "useCaseURL=" + useCaseURL +
                ", useCaseName='" + useCaseName + '\'' +
                '}';
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
