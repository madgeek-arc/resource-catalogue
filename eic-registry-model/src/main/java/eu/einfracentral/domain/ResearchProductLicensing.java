package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import java.net.URL;

public class ResearchProductLicensing {

    /**
     * Research product license name
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation()
    private String researchProductLicenseName;

    /**
     * Research product license URL
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation()
    private URL researchProductLicenseURL;

    public ResearchProductLicensing() {
    }

    public ResearchProductLicensing(String researchProductLicenseName, URL researchProductLicenseURL) {
        this.researchProductLicenseName = researchProductLicenseName;
        this.researchProductLicenseURL = researchProductLicenseURL;
    }

    @Override
    public String toString() {
        return "ResearchProductLicensing{" +
                "researchProductLicenseName='" + researchProductLicenseName + '\'' +
                ", researchProductLicenseURL=" + researchProductLicenseURL +
                '}';
    }

    public String getResearchProductLicenseName() {
        return researchProductLicenseName;
    }

    public void setResearchProductLicenseName(String researchProductLicenseName) {
        this.researchProductLicenseName = researchProductLicenseName;
    }

    public URL getResearchProductLicenseURL() {
        return researchProductLicenseURL;
    }

    public void setResearchProductLicenseURL(URL researchProductLicenseURL) {
        this.researchProductLicenseURL = researchProductLicenseURL;
    }
}
