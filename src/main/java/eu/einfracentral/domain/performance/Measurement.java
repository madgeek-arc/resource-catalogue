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

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "indicator", "from", "to", "value"})
@XmlAccessorType(XmlAccessType.FIELD)
public class Measurement<T extends Comparable> implements Comparable<Measurement<T>>, Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String indicator;
    @XmlElement(required = true)
    private XMLGregorianCalendar from;
    @XmlElement(required = true)
    private XMLGregorianCalendar to;
    @XmlElement(required = true)
    private T value;

    @Override
    public int compareTo(Measurement<T> m) {
        return this.value.compareTo(m.getValue());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getIndicator() {
        return indicator;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public XMLGregorianCalendar getFrom() {
        return from;
    }

    public void setFrom(XMLGregorianCalendar from) {
        this.from = from;
    }

    public XMLGregorianCalendar getTo() {
        return to;
    }

    public void setTo(XMLGregorianCalendar to) {
        this.to = to;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
//    public boolean satisfies(Target<T> t) {
//        return t.satisfiedBy(this.value);
//    }
//
}
