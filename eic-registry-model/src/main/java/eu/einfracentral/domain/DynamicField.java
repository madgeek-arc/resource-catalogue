package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DynamicField {

    @XmlAttribute(required = true)
    private int fieldId;

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private String vocabulary = null;

    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    private List<?> values;

    public DynamicField() {}

    public int getFieldId() {
        return fieldId;
    }

    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    public List<?> getValues() {
        return values;
    }

    public void setValues(List<?> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "DynamicField{" +
                "fieldId=" + fieldId +
                ", name='" + name + '\'' +
                ", vocabulary='" + vocabulary + '\'' +
                ", values=" + values +
                '}';
    }
}
