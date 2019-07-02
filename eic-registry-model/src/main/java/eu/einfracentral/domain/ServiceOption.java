package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceOption implements Identifiable {


    // Option Basic Information
    /**
     * Identifier of the service option.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "(required)", required = true)
    private String id;

    /**
     * Name of the service option.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "(required)", required = true)
    private String name;

    /**
     * Webpage with information about the service option.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "(required)", required = true)
    private URL url;

    /**
     * The description of the service option.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "(required)", required = true)
    private String description;

    /**
     * Link to the logo/visual identity of the service provider.
     */
    @XmlElement
    @ApiModelProperty(position = 5, example = "(optional)")
    private URL logo;

    public ServiceOption(){

    }

    public ServiceOption(ServiceOption option) {
        this.id = option.id;
        this.name = option.name;
        this.url = option.url;
        this.description = option.description;
        this.logo = option.logo;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }
}
