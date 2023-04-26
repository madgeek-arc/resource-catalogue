package eu.einfracentral.domain.interoperabilityRecord.configurationTemplates;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ParamValuePair {

    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
    private String param;

    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String value;

    public ParamValuePair() {
    }

    public ParamValuePair(String param, String value) {
        this.param = param;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ParamValue{" +
                "param='" + param + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}