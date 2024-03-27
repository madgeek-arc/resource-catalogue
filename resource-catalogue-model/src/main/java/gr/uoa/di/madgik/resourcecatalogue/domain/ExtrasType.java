package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class ExtrasType {

    @XmlAttribute
    public String key;

    @XmlValue
    public String value;
}