package eu.einfracentral.dto;

public class Value {

    String id;
    String name;
    String parentId = null;

    public Value() {
    }

    public Value(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Value(String id, String name, String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
