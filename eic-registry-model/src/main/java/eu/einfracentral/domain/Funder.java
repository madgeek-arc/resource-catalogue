package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Funder implements Identifiable {

    @XmlElement
    private String id;

    @XmlElement
    private String name;

    @XmlElement
    private URL logo;

    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private List<String> services;


    public Funder() {
    }

    public Funder(String id, String name, URL logo, List<String> services) {
        this.id = id;
        this.name = name;
        this.logo = logo;
        this.services = services;
    }

    @Override
    public String toString() {
        return "Funder{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", logo=" + logo +
                ", services=" + services +
                '}';
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String s) {
        this.id = s;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
