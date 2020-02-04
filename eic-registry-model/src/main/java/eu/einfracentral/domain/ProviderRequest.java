package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderRequest implements Identifiable {

    @XmlElement
    @FieldValidation
    private String id;

    @XmlElement
    @FieldValidation
    private EmailMessage message;

    @XmlElement
    @FieldValidation
    private String date;

    @XmlElement
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    @XmlElement
    @FieldValidation
    private boolean isRead;

    public ProviderRequest() {
    }

    public ProviderRequest(String id, EmailMessage message, String date, String providerId, boolean isRead) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.providerId = providerId;
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "ProviderRequest{" +
                "id='" + id + '\'' +
                ", message=" + message +
                ", date=" + date +
                ", providerId='" + providerId + '\'' +
                ", status=" + isRead +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public EmailMessage getMessage() {
        return message;
    }

    public void setMessage(EmailMessage message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }
}
