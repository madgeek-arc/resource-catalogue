package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class AlternativeIdentifier {

    @XmlElement()
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private String type;

    @XmlElement()
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String value;

    public AlternativeIdentifier() {
    }

    public AlternativeIdentifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "AlternativeIdentifier{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
