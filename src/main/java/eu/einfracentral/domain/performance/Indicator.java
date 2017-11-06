package eu.einfracentral.domain.performance;

import eu.einfracentral.domain.Identifiable;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "description"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Indicator implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String description;
    @XmlElement(required = true)
    private Dimension[] dimensions;
    @XmlElement(required = true)
    private Unit unit;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Dimension[] getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimension[] dimensions) {
        this.dimensions = dimensions;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

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
