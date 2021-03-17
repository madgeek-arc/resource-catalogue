package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class VocabularyEntryRequest {

    private static final Logger logger = LogManager.getLogger(User.class);

    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
    private String userId;

    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true, containsId = true, idClass = Service.class)
    private String resourceId;

    @XmlElement
    @ApiModelProperty(position = 3)
    @FieldValidation(nullable = true, containsId = true, idClass = Provider.class)
    private String providerId;

    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private Date dateOfRequest;

    @XmlElement(required = true)
    @ApiModelProperty(position = 5, required = true)
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
