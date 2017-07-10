package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {
//    "id", "min", "max"
})
@XmlAccessorType(XmlAccessType.FIELD)

public class Target
//    <T extends JAXBComparable<T>>
{

//    @XmlElement(required = true)
//    private int id;
//
//    @XmlElement(required = true)
//    private T min;
//
//    @XmlElement(required = true)
//    private T max;
//
//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//    public T getMin() {
//        return min;
//    }
//
//    public void setMin(T min) {
//        this.min = min;
//    }
//
//    public T getMax() {
//        return max;
//    }
//
//    public void setMax(T max) {
//        this.max = max;
//    }
//
//    public boolean satisfiedBy(Measurement<T> m) {
//        return m.getValue().compareTo(min) * max.compareTo(m.getValue()) > 0;
//    }
//
//    public boolean satisfiedBy(Collection<Measurement<T>> measurementCollection) {
//        for (Measurement<T> m : measurementCollection) {
//            if (!satisfiedBy(m)) {
//                return false;
//            }
//        }
//        return true;
//    }
}
