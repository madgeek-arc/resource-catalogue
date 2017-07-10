package eu.einfracentral.domain.performance;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {
//    "id", "description", "targets", "timeGranularity", "measurements"
})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Indicator
//<T extends JAXBComparable<T>>
{
//
//    @XmlElement(required = true)
//    private int id;
//
//    @XmlElement(required = true)
//    private String description;
//
//    @XmlElementWrapper(required = true)
//    @XmlElement(name = "target")
//    private List<Target<T>> targets;
//
//    @XmlElement(required = true)
//    private ChronoUnit timeGranularity;
//
//    @XmlElementWrapper(required = true)
//    @XmlElement(name = "measurement")
//    private List<Measurement<T>> measurements;
//
//    public boolean satisfiedWithin(XMLGregorianCalendar from, XMLGregorianCalendar to) {
//        boolean ret = false;
//        for (Target<T> t : targets) {
//            boolean allMeasurementsAreSatisfactoryForTarget = false;
//            for (Measurement<T> m : measurements) {
//                if (m.getTime().toGregorianCalendar().compareTo(from.toGregorianCalendar()) < 0 &&
//                    m.getTime().toGregorianCalendar().compareTo(to.toGregorianCalendar()) > 0) {
//                    allMeasurementsAreSatisfactoryForTarget = true;
//                    allMeasurementsAreSatisfactoryForTarget &= t.satisfiedBy(m);
//                }
//                if (allMeasurementsAreSatisfactoryForTarget) {
//                    ret = allMeasurementsAreSatisfactoryForTarget;
//                    break;
//                }
//            }
//            if (satisfied) {
//                break;
//            }
//        }
//        return satisfied;
//    }
}
