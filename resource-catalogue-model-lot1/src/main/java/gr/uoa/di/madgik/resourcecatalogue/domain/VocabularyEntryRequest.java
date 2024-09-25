package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class VocabularyEntryRequest {

    private static final Logger logger = LogManager.getLogger(User.class);

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
    private Date dateOfRequest;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String resourceType;


    public VocabularyEntryRequest() {
    }

    public VocabularyEntryRequest(String userId, String resourceId, String providerId, Date dateOfRequest, String resourceType) {
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

    public Date getDateOfRequest() {
        return dateOfRequest;
    }

    public void setDateOfRequest(Date dateOfRequest) {
        this.dateOfRequest = dateOfRequest;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
