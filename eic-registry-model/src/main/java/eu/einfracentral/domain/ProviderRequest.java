package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

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
    private Date date;

    @XmlElement
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    @XmlElement
    @FieldValidation
    private boolean status;

    public ProviderRequest() {
    }

    public ProviderRequest(String id, EmailMessage message, Date date, String providerId, boolean status) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.providerId = providerId;
        this.status = status;
    }

    @Override
    public String toString() {
        return "ProviderRequest{" +
                "id='" + id + '\'' +
                ", message=" + message +
                ", date=" + date +
                ", providerId='" + providerId + '\'' +
                ", status=" + status +
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
