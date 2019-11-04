package eu.einfracentral.dto;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public class ProviderInfo {

    private String providerId;
    private String providerName;
    private String providerAcronym;

    public ProviderInfo() {
    }

    public ProviderInfo(String providerId, String providerName, String providerAcronym) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerAcronym = providerAcronym;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderAcronym() {
        return providerAcronym;
    }

    public void setProviderAcronym(String providerAcronym) {
        this.providerAcronym = providerAcronym;
    }
}
