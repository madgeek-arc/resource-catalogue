package gr.uoa.di.madgik.resourcecatalogue.dto;

import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public class ScientificDomain {

    private Vocabulary domain;
    private Vocabulary subdomain;

    public ScientificDomain() {
    }

    public ScientificDomain(Vocabulary domain, Vocabulary subdomain) {
        this.domain = domain;
        this.subdomain = subdomain;
    }

    public Vocabulary getDomain() {
        return domain;
    }

    public void setDomain(Vocabulary domain) {
        this.domain = domain;
    }

    public Vocabulary getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(Vocabulary subdomain) {
        this.subdomain = subdomain;
    }
}
