package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "min", "max"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Target<T extends Comparable<T>> {

    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = true, nillable = false)
    private T min;

    @XmlElement(required = true, nillable = false)
    private T max;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public T getMin() {
        return min;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public T getMax() {
        return max;
    }

    public void setMax(T max) {
        this.max = max;
    }

    public boolean satisfiedBy(T value) {
        return value.compareTo(min) * max.compareTo(value) > 0;
    }
}
