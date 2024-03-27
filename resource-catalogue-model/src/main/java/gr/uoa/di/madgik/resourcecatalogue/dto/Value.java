package gr.uoa.di.madgik.resourcecatalogue.dto;

public class Value {

    String id;
    String name;

    public Value() {
    }

    public Value(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
