package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MultimediaPair {

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElement(name = "multimediaURL", required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation()
    private URL multimediaURL;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElement(name = "multimediaName")
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String multimediaName;

    public MultimediaPair(){
    }

    public MultimediaPair(URL multimediaURL, String multimediaName) {
        this.multimediaURL = multimediaURL;
        this.multimediaName = multimediaName;
    }

    @Override
    public String toString() {
        return "Multimedia{" +
                "multimediaURL=" + multimediaURL +
                ", multimediaName='" + multimediaName + '\'' +
                '}';
    }

    public URL getMultimediaURL() {
        return multimediaURL;
    }

    public void setMultimediaURL(URL multimediaURL) {
        this.multimediaURL = multimediaURL;
    }

    public String getMultimediaName() {
        return multimediaName;
    }

    public void setMultimediaName(String multimediaName) {
        this.multimediaName = multimediaName;
    }
}
