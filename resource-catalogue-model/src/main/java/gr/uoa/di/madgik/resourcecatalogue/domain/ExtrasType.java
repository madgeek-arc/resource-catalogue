package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

public class ExtrasType {

    @XmlAttribute
    public String key;

    @XmlValue
    public String value;
}