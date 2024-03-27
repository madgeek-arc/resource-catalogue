package gr.uoa.di.madgik.domain;

import gr.uoa.di.madgik.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import java.net.URL;
import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchProductLicensing that = (ResearchProductLicensing) o;
        return Objects.equals(researchProductLicenseName, that.researchProductLicenseName) && Objects.equals(researchProductLicenseURL, that.researchProductLicenseURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(researchProductLicenseName, researchProductLicenseURL);
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
