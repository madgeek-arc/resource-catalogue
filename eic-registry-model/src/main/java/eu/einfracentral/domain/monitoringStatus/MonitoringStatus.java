package eu.einfracentral.domain.monitoringStatus;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MonitoringStatus {

    private String date;
    private String availability;
    private String reliability;
    private String unknown;
    private String uptime;
    private String downtime;

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
}
