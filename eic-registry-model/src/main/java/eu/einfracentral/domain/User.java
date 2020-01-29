package eu.einfracentral.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class User implements Identifiable {

    private static final Logger logger = LogManager.getLogger(User.class);

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
        if (auth == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (auth instanceof OIDCAuthenticationToken) {
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
        } else if (auth.isAuthenticated()) {
            this.name = auth.getName();
            this.id = "";
            this.email = "";
            this.surname = "";
            logger.warn("Authenticated User has missing information: {}", auth);
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                '}';
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

    @JsonIgnore
    public String getFullName() {
        return String.format("%s %s", this.name, this.surname);
    }
}
