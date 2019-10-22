package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Funder implements Identifiable {

    @XmlElement
    private String id;

    @XmlElement
    private String fundingOrganisation;

    @XmlElement
    private String organisationLocalLanguage;

    @XmlElement
    private String acronym;

    @XmlElement
    private String country;


    public Funder() {
    }

    public Funder(String id, String fundingOrganisation, String organisationLocalLanguage, String acronym, String country) {
        this.id = id;
        this.fundingOrganisation = fundingOrganisation;
        this.organisationLocalLanguage = organisationLocalLanguage;
        this.acronym = acronym;
        this.country = country;
    }

    @Override
    public String toString() {
        return "Funder{" +
                "id='" + id + '\'' +
                ", fundingOrganisation='" + fundingOrganisation + '\'' +
                ", organisationLocalLanguage='" + organisationLocalLanguage + '\'' +
                ", acronym='" + acronym + '\'' +
                ", country='" + country + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String s) {
        this.id = s;
    }

    public String getFundingOrganisation() {
        return fundingOrganisation;
    }

    public void setFundingOrganisation(String fundingOrganisation) {
        this.fundingOrganisation = fundingOrganisation;
    }

    public String getOrganisationLocalLanguage() {
        return organisationLocalLanguage;
    }

    public void setOrganisationLocalLanguage(String organisationLocalLanguage) {
        this.organisationLocalLanguage = organisationLocalLanguage;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
