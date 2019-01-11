package eu.einfracentral.domain;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class User implements Identifiable {

    @XmlElement
    private String id;

    @XmlElement
    private String email;

    @XmlElement
    private String name;

    @XmlElement
    private String surname;

    public User() {
    }

    public User(String id, String email, String name, String surname) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.surname = surname;
    }

    public User(Authentication auth) {
        if (auth instanceof OIDCAuthenticationToken) {
            this.id = ((OIDCAuthenticationToken) auth).getUserInfo().getSub();
            if (this.id == null) {
                this.id = "";
            }
            this.email = ((OIDCAuthenticationToken) auth).getUserInfo().getEmail();
            if (this.email == null) {
                this.email = "";
            }
            this.name = ((OIDCAuthenticationToken) auth).getUserInfo().getGivenName();
            this.surname = ((OIDCAuthenticationToken) auth).getUserInfo().getFamilyName();
        } else {
            throw new RuntimeException("Could not create user. Authentication is not an instance of OIDCAuthentication");
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFullName() {
        return String.format("%s %s", this.name, this.surname);
    }
}
