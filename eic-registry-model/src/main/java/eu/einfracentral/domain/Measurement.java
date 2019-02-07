package eu.einfracentral.domain;

import org.springframework.util.MultiValueMap;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Measurement<T> implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String indicatorId;
    @XmlElement(required = true)
    private String serviceId;
    @XmlElement(required = true)
    private String value; //value can be PCT, NUM or BOOL
    @XmlElement
    private XMLGregorianCalendar time;

    //TODO: we need vocabulary type of location, so to not have problems with the design tool later!
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "location")
    private List<String> locations;

//    @XmlElementWrapper(name="dimensionValues", required = true)
//    @XmlElement(name="dimensionValue")
//    private List<String> dimensionValues;


    public Measurement() {

    }

    public Measurement(Measurement measurement) {
        this.id = measurement.getId();
        this.indicatorId = measurement.getIndicatorId();
        this.serviceId = measurement.getServiceId();
        this.value = measurement.getValue();
        this.time = measurement.getTime();
        this.locations = measurement.getLocations();
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getIndicatorId() {
        return indicatorId;
    }

    public void setIndicatorId(String indicatorId) {
        this.indicatorId = indicatorId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public XMLGregorianCalendar getTime() {
        return time;
    }

    public void setTime(XMLGregorianCalendar time) {
        this.time = time;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

}
