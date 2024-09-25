package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import java.net.URL;
import java.util.Objects;

public class ResearchProductMetadataLicensing {

    /**
     * Research Product Metadata License Name
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private String researchProductMetadataLicenseName;

    /**
     * Research Product Metadata License URL
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private URL researchProductMetadataLicenseURL;

    public ResearchProductMetadataLicensing() {
    }

    public ResearchProductMetadataLicensing(String researchProductMetadataLicenseName, URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchProductMetadataLicensing that = (ResearchProductMetadataLicensing) o;
        return Objects.equals(researchProductMetadataLicenseName, that.researchProductMetadataLicenseName) && Objects.equals(researchProductMetadataLicenseURL, that.researchProductMetadataLicenseURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(researchProductMetadataLicenseName, researchProductMetadataLicenseURL);
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
