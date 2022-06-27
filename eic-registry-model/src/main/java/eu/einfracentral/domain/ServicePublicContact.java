
package eu.einfracentral.domain;

import eu.einfracentral.annotation.EmailValidation;
import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.PhoneValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;


@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServicePublicContact {


    // Contact Basic Information
    /**
     * First Name of the Resource's contact person to be displayed at the portal.
     */
    @XmlElement
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private String firstName;

    /**
     * Last Name of the Resource's contact person to be displayed at the portal.
     */
    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String lastName;

    /**
     * Email of the Resource's contact person or a generic email of the Provider to be displayed at the portal.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @EmailValidation
    private String email;

    /**
     * Telephone of the Resource's contact person to be displayed at the portal.
     */
    @XmlElement
    @ApiModelProperty(position = 4)
    @PhoneValidation(nullable = true)
    private String phone;

    /**
     * Position of the Resource's contact person to be displayed at the portal.
     */
    @XmlElement
    @ApiModelProperty(position = 5)
    @FieldValidation(nullable = true)
    private String position;

    /**
     * The organisation to which the contact is affiliated.
     */
    @XmlElement
    @ApiModelProperty(position = 6)
    @FieldValidation(nullable = true)
    private String organisation;

    public ServicePublicContact() {
    }

    public ServicePublicContact(String firstName, String lastName, String email, String phone, String position, String organisation) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServicePublicContact that = (ServicePublicContact) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(email, that.email) && Objects.equals(phone, that.phone) && Objects.equals(position, that.position) && Objects.equals(organisation, that.organisation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, phone, position, organisation);
    }
}
