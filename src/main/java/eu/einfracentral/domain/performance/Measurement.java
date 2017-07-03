package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.*;
import java.time.Instant;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "time", "value"})
@XmlAccessorType(XmlAccessType.FIELD)

public class Measurement<T extends Comparable<T>> implements Comparable<T> {
    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private Instant time;

    @XmlElement(required = true)
    private T value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean satisfies(Target<T> t) {
        return t.satisfiedBy(this.value);
    }

    @Override
    public int compareTo(T value) {
        return this.value.compareTo(value);
    }
}
