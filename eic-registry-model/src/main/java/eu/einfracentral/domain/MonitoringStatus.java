package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MonitoringStatus {

    // Monitoring Availability
    @XmlElement
    private String date;
    @XmlElement
    private String availability;
    @XmlElement
    private String reliability;
    @XmlElement
    private String unknown;
    @XmlElement
    private String uptime;
    @XmlElement
    private String downtime;

    // Monitoring Status
    @XmlElement
    private String timestamp;
    @XmlElement
    private String value;

    public MonitoringStatus() {
    }

    public MonitoringStatus(String date, String availability, String reliability, String unknown, String uptime, String downtime) {
        this.date = date;
        this.availability = availability;
        this.reliability = reliability;
        this.unknown = unknown;
        this.uptime = uptime;
        this.downtime = downtime;
    }

    public MonitoringStatus(String timestamp, String value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getReliability() {
        return reliability;
    }

    public void setReliability(String reliability) {
        this.reliability = reliability;
    }

    public String getUnknown() {
        return unknown;
    }

    public void setUnknown(String unknown) {
        this.unknown = unknown;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public String getDowntime() {
        return downtime;
    }

    public void setDowntime(String downtime) {
        this.downtime = downtime;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
