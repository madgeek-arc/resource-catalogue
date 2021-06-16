package eu.einfracentral.domain;


public class DynamicField {

    private String name;
    private Object value;
    private int fieldId;

    public DynamicField() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getFieldId() {
        return fieldId;
    }

    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }

    @Override
    public String toString() {
        return "DynamicField{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", fieldId='" + fieldId + '\'' +
                '}';
    }
}
