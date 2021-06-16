package eu.einfracentral.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DynamicField {

    private String name;
    private String type = "java.lang.String";
    private boolean multiplicity = false;
    private Object value;

    public DynamicField() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(boolean multiplicity) {
        this.multiplicity = multiplicity;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DynamicField{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", multiplicity=" + multiplicity +
                ", value=" + value +
                '}';
    }
}
