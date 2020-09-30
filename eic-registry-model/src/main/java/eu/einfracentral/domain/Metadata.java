package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Metadata {

    @XmlElement(defaultValue = "null")
    private String registeredBy;

    @XmlElement(defaultValue = "null")
    private String registeredAt;

    @XmlElement(defaultValue = "null")
    private String modifiedBy;

    @XmlElement(defaultValue = "null")
    private String modifiedAt;

    @XmlElement(defaultValue = "null")
    private String source;

    @XmlElement(defaultValue = "null")
    private String originalId;

    @XmlElement(defaultValue = "null")
    private List<AbstractMap.SimpleEntry<String, Boolean>> terms;


    public Metadata() {
    }

    public Metadata(Metadata metadata) {
        this.registeredBy = metadata.getRegisteredBy();
        this.modifiedBy = metadata.getModifiedBy();
        this.registeredAt = metadata.getRegisteredAt();
        this.modifiedAt = metadata.getModifiedAt();
        this.source = metadata.getSource();
        this.originalId = metadata.getOriginalId();
        this.terms = metadata.getTerms();
    }

    public static Metadata updateMetadata(Metadata metadata, String modifiedBy) {
        Metadata ret;
        if (metadata != null) {
            ret = new Metadata(metadata);
            ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            ret.setModifiedBy(modifiedBy);
        } else {
            ret = createMetadata(modifiedBy);
        }
        return ret;
    }

    public static Metadata updateMetadata(Metadata metadata, String modifiedBy, List<User> users) {
        Metadata ret;
        if (metadata != null) {
            ret = new Metadata(metadata);
            ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            ret.setModifiedBy(modifiedBy);
        } else {
            ret = createMetadata(modifiedBy, users);
        }
        return ret;
    }

    public static Metadata createMetadata(String registeredBy) {
        Metadata ret = new Metadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(registeredBy);
        ret.setModifiedAt(ret.getRegisteredAt());
        return ret;
    }

    public static Metadata createMetadata(String registeredBy, List<User> users) {
        Metadata ret = new Metadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(registeredBy);
        ret.setModifiedAt(ret.getRegisteredAt());
        ret.setTerms(adminAcceptedTerms(users));
        return ret;
    }

    public static Metadata createMetadata(String registeredBy, String originalId, String source, List<AbstractMap.SimpleEntry<String, Boolean>> terms) {
        Metadata metadata = new Metadata();
        metadata.setRegisteredBy(registeredBy);
        metadata.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(registeredBy);
        metadata.setModifiedAt(metadata.getRegisteredAt());
        metadata.setOriginalId(originalId);
        metadata.setSource(source);
        metadata.setTerms(terms);
        return metadata;
    }

    public static List<AbstractMap.SimpleEntry<String, Boolean>> adminAcceptedTerms(List<User> users){
        List<AbstractMap.SimpleEntry<String, Boolean>> termsList = new ArrayList<>();
        boolean firstTime = true;
        for (User user : users){
            if (firstTime){
                AbstractMap.SimpleEntry<String, Boolean> entry = new AbstractMap.SimpleEntry<>(user.getEmail(), true);
                termsList.add(entry);
                firstTime = false;
            } else {
                AbstractMap.SimpleEntry<String, Boolean> entry = new AbstractMap.SimpleEntry<>(user.getEmail(), false);
                termsList.add(entry);
            }
        }
        return termsList;
    }


    @Override
    public String toString() {
        return "Metadata{" +
                "registeredBy='" + registeredBy + '\'' +
                ", registeredAt='" + registeredAt + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", modifiedAt='" + modifiedAt + '\'' +
                ", source='" + source + '\'' +
                ", originalId='" + originalId + '\'' +
                ", terms=" + terms +
                '}';
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public List<AbstractMap.SimpleEntry<String, Boolean>> getTerms() {
        return terms;
    }

    public void setTerms(List<AbstractMap.SimpleEntry<String, Boolean>> terms) {
        this.terms = terms;
    }
}
