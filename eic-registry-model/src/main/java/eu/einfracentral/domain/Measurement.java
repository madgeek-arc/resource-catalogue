package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Measurement implements Identifiable {
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "Measurement's ID")
    private String id;
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "Indicator's ID")
    private String indicatorId;
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "Service's ID")
    private String serviceId;
    @XmlElement
    @ApiModelProperty(position = 4, example = "Timestamp of the specific measurement")
    private XMLGregorianCalendar time;
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "location")
    @ApiModelProperty(position = 5, example = "['l1', 'l2']")
    private List<String> locations;
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, example = "Actual value of the measurement")
    private String value;


    //TODO: Make at least one of time, locations mandatory!


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
