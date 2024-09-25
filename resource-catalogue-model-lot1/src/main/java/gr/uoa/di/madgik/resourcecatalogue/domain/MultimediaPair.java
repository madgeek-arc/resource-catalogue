package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MultimediaPair {

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation()
    private URL multimediaURL;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true)
    private String multimediaName;

    public MultimediaPair() {
    }

    public MultimediaPair(URL multimediaURL, String multimediaName) {
        this.multimediaURL = multimediaURL;
        this.multimediaName = multimediaName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultimediaPair that = (MultimediaPair) o;
        return Objects.equals(multimediaURL, that.multimediaURL) && Objects.equals(multimediaName, that.multimediaName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multimediaURL, multimediaName);
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
