package eu.einfracentral.domain.performance;

import eu.einfracentral.domain.Identifiable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "indicator", "value", "time", "location"})
@XmlAccessorType(XmlAccessType.FIELD)
public class Measurement<T> implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private Indicator indicator;
    @XmlElement(required = true)
    private Object value;
    @XmlElement
    private XMLGregorianCalendar time;
    @XmlElement
    private String[] location;

   @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Indicator getIndicator() {
        return indicator;
    }

    public void setIndicator(Indicator indicator) {
        this.indicator = indicator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public XMLGregorianCalendar getTime() {
        return time;
    }

    public void setTime(XMLGregorianCalendar time) {
        this.time = time;
    }

    public String[] getLocation() {
        return location;
    }

    public void setLocation(String[] location) {
        this.location = location;
    }

    //    public boolean satisfies(Target<T> t) {
//        return t.satisfiedBy(this.value);
//    }
//
//    @Override
//    public int compareTo(Measurement<T> m) {
//        return this.value.compareTo(m.getValue());
//    }

}
