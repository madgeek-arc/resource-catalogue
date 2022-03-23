package eu.einfracentral.dto;

import java.util.Objects;

public class Value {

    String id;
    String name;
    String parentId = null;
    String tagline = null;
    String image = null;

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

    public Value(String id, String name, String parentId, String tagline, String image) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.tagline = tagline;
        this.image = image;
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

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value = (Value) o;
        return id.equals(value.id) && name.equals(value.name) && Objects.equals(parentId, value.parentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, parentId);
    }
}
