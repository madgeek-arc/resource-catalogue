package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

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
    private XMLGregorianCalendar date;

    @XmlElement
    @FieldValidation
    private String providerId;

    public ProviderRequest() {
    }

    public ProviderRequest(String id, EmailMessage message, XMLGregorianCalendar date, String providerId) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.providerId = providerId;
    }

    @Override
    public String toString() {
        return "ProviderRequest{" +
                "id='" + id + '\'' +
                ", message=" + message +
                ", date=" + date +
                ", providerId='" + providerId + '\'' +
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

    public XMLGregorianCalendar getDate() {
        return date;
    }

    public void setDate(XMLGregorianCalendar date) {
        this.date = date;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
