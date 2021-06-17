package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DynamicField {

    @XmlElement
    private String name;

    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    private List<Object> values;

    @XmlElement
    private int fieldId;

    public DynamicField() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getValue() {
        return values;
    }

    public void setValue(List<Object> values) {
        this.values = values;
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
                ", values=" + values +
                ", fieldId=" + fieldId +
                '}';
    }
}
