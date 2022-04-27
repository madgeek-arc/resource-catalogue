package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import java.net.URL;

public class ResearchProductMetadataLicensing {

    /**
     * Research Product Metadata License Name
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation()
    private String researchProductMetadataLicenseName;

    /**
     * Research Product Metadata License URL
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation()
    private URL researchProductMetadataLicenseURL;

    public ResearchProductMetadataLicensing() {
    }

    public ResearchProductMetadataLicensing(String researchProductMetadataLicenseName, URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }

    @Override
    public String toString() {
        return "ResearchProductMetadataLicensing{" +
                "researchProductMetadataLicenseName='" + researchProductMetadataLicenseName + '\'' +
                ", researchProductMetadataLicenseURL=" + researchProductMetadataLicenseURL +
                '}';
    }

    public String getResearchProductMetadataLicenseName() {
        return researchProductMetadataLicenseName;
    }

    public void setResearchProductMetadataLicenseName(String researchProductMetadataLicenseName) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
    }

    public URL getResearchProductMetadataLicenseURL() {
        return researchProductMetadataLicenseURL;
    }

    public void setResearchProductMetadataLicenseURL(URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }
}
