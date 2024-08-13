package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.EmailValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.PhoneValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderMainContact {


    // Contact Basic Information
    /**
     * First Name of the Provider's main contact person/Provider manager.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String firstName;

    /**
     * Last Name of the Provider's main contact person/Provider manager.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String lastName;

    /**
     * Email of the Provider's main contact person/Provider manager.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @EmailValidation
    private String email;

    /**
     * Phone of the Provider's main contact person/Provider manager.
     */
    @XmlElement
    @Schema
    @PhoneValidation(nullable = true)
    private String phone;

    /**
     * Position of the Provider's main contact person/Provider manager.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String position;

    public ProviderMainContact() {
    }

    public ProviderMainContact(String firstName, String lastName, String email, String phone, String position) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.position = position;
    }

    @Override
    public String toString() {
        return "ProviderMainContact{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", position='" + position + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderMainContact that = (ProviderMainContact) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(email, that.email) && Objects.equals(phone, that.phone) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, phone, position);
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
}

