package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceMainContact {


    // Contact Basic Information
    /**
     * First Name of the Resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "String (required)", required = true)
    @FieldValidation
    private String firstName;

    /**
     * Last Name of the Resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    @FieldValidation
    private String lastName;

    /**
     * Email of the Resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "String (required)", required = true)
    @FieldValidation
    private String email;

    /**
     * Telephone of the Resource's main contact person/manager.
     */
    @XmlElement
    @ApiModelProperty(position = 4, example = "String (optional)")
    @FieldValidation(nullable = true)
    private String phone;

    /**
     * Position of the Resource's main contact person/manager.
     */
    @XmlElement
    @ApiModelProperty(position = 5, example = "String (optional)")
    @FieldValidation(nullable = true)
    private String position;

    /**
     * The organisation to which the contact is affiliated.
     */
    @XmlElement
    @ApiModelProperty(position = 6, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Provider.class)
    private String organisation;

    public ServiceMainContact() {
    }

    public ServiceMainContact(String firstName, String lastName, String email, String phone, String position, String organisation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.organisation = organisation;
    }

    @Override
    public String toString() {
        return "ServiceMainContact{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", position='" + position + '\'' +
                ", organisation='" + organisation + '\'' +
                '}';
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }
}