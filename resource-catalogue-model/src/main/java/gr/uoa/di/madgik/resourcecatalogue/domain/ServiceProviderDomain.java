package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceProviderDomain {

    /**
     * The branch of science, scientific discipline that is related to the resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_DOMAIN)
    private String scientificDomain;

    /**
     * The sub-branch of science, scientific sub-discipline that is related to the resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_SUBDOMAIN)
    private String scientificSubdomain;

    public ServiceProviderDomain() {
    }

    public ServiceProviderDomain(String scientificDomain, String scientificSubdomain) {
        this.scientificDomain = scientificDomain;
        this.scientificSubdomain = scientificSubdomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceProviderDomain that = (ServiceProviderDomain) o;
        return Objects.equals(scientificDomain, that.scientificDomain) && Objects.equals(scientificSubdomain, that.scientificSubdomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scientificDomain, scientificSubdomain);
    }

    @Override
    public String toString() {
        return "ProviderDomains{" +
                "scientificDomain='" + scientificDomain + '\'' +
                ", scientificSubdomain='" + scientificSubdomain + '\'' +
                '}';
    }

    public String getScientificDomain() {
        return scientificDomain;
    }

    public void setScientificDomain(String scientificDomain) {
        this.scientificDomain = scientificDomain;
    }

    public String getScientificSubdomain() {
        return scientificSubdomain;
    }

    public void setScientificSubdomain(String scientificSubdomain) {
        this.scientificSubdomain = scientificSubdomain;
    }
}
