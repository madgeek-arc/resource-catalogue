package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class VocabularyEntryRequest {

    private static final Logger logger = LogManager.getLogger(VocabularyEntryRequest.class);

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String userId;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, containsResourceId = true)
    private String resourceId;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Provider.class)
    private String providerId;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String dateOfRequest;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String resourceType;


    public VocabularyEntryRequest() {
    }

    public VocabularyEntryRequest(String userId, String resourceId, String providerId, String dateOfRequest, String resourceType) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.providerId = providerId;
        this.dateOfRequest = dateOfRequest;
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        return "VocabularyEntryRequest{" +
                "userId='" + userId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", providerId='" + providerId + '\'' +
                ", dateOfRequest=" + dateOfRequest +
                ", resourceType='" + resourceType + '\'' +
                '}';
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getDateOfRequest() {
        return dateOfRequest;
    }

    public void setDateOfRequest(String dateOfRequest) {
        this.dateOfRequest = dateOfRequest;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
