package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderMerilDomain {


    // Provider's Location Information
    /**
     * MERIL scientific domain classification.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_DOMAIN)
    private String merilScientificDomain;

    /**
     * MERIL scientific subdomain classification.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_SUBDOMAIN)
    private String merilScientificSubdomain;

    public ProviderMerilDomain() {
    }

    public ProviderMerilDomain(String merilScientificDomain, String merilScientificSubdomain) {
        this.merilScientificDomain = merilScientificDomain;
        this.merilScientificSubdomain = merilScientificSubdomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderMerilDomain that = (ProviderMerilDomain) o;
        return Objects.equals(merilScientificDomain, that.merilScientificDomain) && Objects.equals(merilScientificSubdomain, that.merilScientificSubdomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merilScientificDomain, merilScientificSubdomain);
    }

    @Override
    public String toString() {
        return "ProviderMerilDomain{" +
                "merilScientificDomain='" + merilScientificDomain + '\'' +
                ", merilScientificSubdomain='" + merilScientificSubdomain + '\'' +
                '}';
    }

    public String getMerilScientificDomain() {
        return merilScientificDomain;
    }

    public void setMerilScientificDomain(String merilScientificDomain) {
        this.merilScientificDomain = merilScientificDomain;
    }

    public String getMerilScientificSubdomain() {
        return merilScientificSubdomain;
    }

    public void setMerilScientificSubdomain(String merilScientificSubdomain) {
        this.merilScientificSubdomain = merilScientificSubdomain;
    }
}
