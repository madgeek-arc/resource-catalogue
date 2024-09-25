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
public class ProviderLocation {


    // Provider's Location Information
    /**
     * Street and Number of incorporation or Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile providers.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String streetNameAndNumber;

    /**
     * Postal code of incorporation or Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile providers.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String postalCode;

    /**
     * City of incorporation or Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile providers.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String city;

    /**
     * Region of incorporation or Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile providers.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String region;

    /**
     * Country of incorporation or Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile providers.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.COUNTRY)
    private String country;

    public ProviderLocation() {
    }

    public ProviderLocation(String streetNameAndNumber, String postalCode, String city, String region, String country) {
        this.streetNameAndNumber = streetNameAndNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderLocation that = (ProviderLocation) o;
        return Objects.equals(streetNameAndNumber, that.streetNameAndNumber) && Objects.equals(postalCode, that.postalCode) && Objects.equals(city, that.city) && Objects.equals(region, that.region) && Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streetNameAndNumber, postalCode, city, region, country);
    }

    @Override
    public String toString() {
        return "ProviderLocation{" +
                "streetNameAndNumber='" + streetNameAndNumber + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", city='" + city + '\'' +
                ", region='" + region + '\'' +
                ", country='" + country + '\'' +
                '}';
    }

    public String getStreetNameAndNumber() {
        return streetNameAndNumber;
    }

    public void setStreetNameAndNumber(String streetNameAndNumber) {
        this.streetNameAndNumber = streetNameAndNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
