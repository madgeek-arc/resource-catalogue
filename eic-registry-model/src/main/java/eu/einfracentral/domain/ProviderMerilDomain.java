package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderMerilDomain {


    // Provider's Location Information
    /**
     * MERIL scientific domain classification.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_DOMAIN)
    private String merilScientificDomain;

    /**
     * MERIL scientific subdomain classification.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
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
