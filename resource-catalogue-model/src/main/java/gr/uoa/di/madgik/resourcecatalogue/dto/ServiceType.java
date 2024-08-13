package gr.uoa.di.madgik.resourcecatalogue.dto;

public class ServiceType {

    private String date;
    private String name;
    private String title;
    private String description;

    public ServiceType() {
    }

    public ServiceType(String date, String name, String title, String description) {
        this.date = date;
        this.name = name;
        this.title = title;
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
