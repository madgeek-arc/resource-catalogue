package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description", "target", "timeGranularity", "measurements"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Indicator<T extends Comparable<T>> {

    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private String description;

    @XmlElement(required = true)
    private Target target;

    @XmlElement(required = true)
    private ChronoUnit timeGranularity;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "measurement")
    private List<Measurement<T>> measurements;

    public boolean satisfiedWithin(XMLGregorianCalendar from, XMLGregorianCalendar to) {
        for (Measurement<T> m : measurements) {

            if (m.getTime().toGregorianCalendar().compareTo(from.toGregorianCalendar()) < 0 &&
                m.getTime().toGregorianCalendar().compareTo(to.toGregorianCalendar()) > 0) {
                continue;
            }
            if (!m.satisfies(target)) {
                return false;
            }
        }
        return true;
    }
}
