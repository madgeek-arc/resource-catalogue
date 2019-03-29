package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class RangeValue {

    @XmlElement
    private String fromValue;

    @XmlElement
    private String toValue;

    public RangeValue() {}

    public RangeValue(String fromValue, String toValue) {
        this.fromValue = fromValue;
        this.toValue = toValue;
    }

    public RangeValue(RangeValue rangeValue) {
        this.fromValue = rangeValue.getFromValue();
        this.toValue = rangeValue.getToValue();
    }

    public String getFromValue() {
        return fromValue;
    }

    public void setFromValue(String fromValue) {
        this.fromValue = fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }
}
