package gr.uoa.di.madgik.resourcecatalogue.dto;

public class MonitoringStatus {

    // Monitoring Availability
    private String date;
    private String availability;
    private String reliability;
    private String unknown;
    private String uptime;
    private String downtime;

    // Monitoring Status
    private String timestamp;
    private String value;

    public MonitoringStatus() {
        // no-arg constructor
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
