package eu.einfracentral.ui;

public class FieldIdName {

    Integer id;
    String name = null;

    public FieldIdName() {}

    public FieldIdName(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
