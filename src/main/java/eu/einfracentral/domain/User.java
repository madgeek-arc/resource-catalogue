package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

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

}
