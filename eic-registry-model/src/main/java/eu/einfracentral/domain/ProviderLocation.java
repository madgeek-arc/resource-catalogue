package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static eu.einfracentral.utils.ValidationLengths.*;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderLocation {


    // Provider's Location Information
    /**
     * Provider's location name.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String name;

    /**
     * Provider's location street.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String street;

    /**
     * Provider's location street number.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String number;

    /**
     * Provider's location postal code.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String postalCode;

    /**
     * Provider's location city.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String city;

    /**
     * Provider's location region.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String region;

    public ProviderLocation() {
    }

    public ProviderLocation(String name, String street, String number, String postalCode, String city, String region) {
        this.name = name;
        this.street = street;
        this.number = number;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
    }

    @Override
    public String toString() {
        return "ProviderLocation{" +
                "name='" + name + '\'' +
                ", street='" + street + '\'' +
                ", number='" + number + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", city='" + city + '\'' +
                ", region='" + region + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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
}
