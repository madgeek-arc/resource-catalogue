package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static eu.einfracentral.utils.ValidationLengths.FIELD_LENGTH;
import static eu.einfracentral.utils.ValidationLengths.FIELD_LENGTH_SMALL;


@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Contact {


    // Contact Basic Information
    /**
     * First Name of the service/resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String firstName;

    /**
     * Last Name of the service/resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String lastName;

    /**
     * Email of the service/resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH)
    private String email;

    /**
     * Telephone of the service/resource's main contact person/manager.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String tel;

    /**
     * Position of the service/resource's main contact person/manager.
     */
    @XmlElement
    @ApiModelProperty(position = 5, example = "String (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH_SMALL)
    private String position;

    public Contact() {
    }

    public Contact(String firstName, String lastName, String email, String tel, String position) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tel = tel;
        this.position = position;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", tel='" + tel + '\'' +
                ", position='" + position + '\'' +
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

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
