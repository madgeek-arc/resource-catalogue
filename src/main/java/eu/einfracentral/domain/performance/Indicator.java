package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description", "target", "timeGranularity", "measurements"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Indicator<T extends Comparable<T>> {

    @XmlElement(required = true, nillable = false)
    private int id;

    @XmlElement(required = true, nillable = false)
    private String description;

    @XmlElement(required = true, nillable = false)
    private Target target;

    @XmlElement(required = true, nillable = false)
    private ChronoUnit timeGranularity;

    @XmlElement(required = true, nillable = false)
    private List<Measurement<T>> measurements;

    public boolean satisfiedWithin(Instant from, Instant to) {
        for (Measurement<T> m : measurements) {
            if (from.isAfter(m.getTime()) || to.isBefore(m.getTime())) {
                continue;
            }
            if (!m.satisfies(target)) {
                return false;
            }
        }
        return true;
    }
}
