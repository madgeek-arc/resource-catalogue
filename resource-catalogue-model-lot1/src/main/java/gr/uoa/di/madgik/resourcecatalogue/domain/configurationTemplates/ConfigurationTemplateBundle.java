package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ConfigurationTemplateBundle extends Bundle<ConfigurationTemplate> {

    public ConfigurationTemplateBundle() {
    }

    public ConfigurationTemplateBundle(ConfigurationTemplate configurationTemplate) {
        this.setConfigurationTemplate(configurationTemplate);
        this.setMetadata(null);
    }

    public ConfigurationTemplateBundle(ConfigurationTemplate configurationTemplate, Metadata metadata) {
        this.setConfigurationTemplate(configurationTemplate);
        this.setMetadata(metadata);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @XmlElement(name = "configurationTemplate")
    public ConfigurationTemplate getConfigurationTemplate() {
        return this.getPayload();
    }

    public void setConfigurationTemplate(ConfigurationTemplate configurationTemplate) {
        this.setPayload(configurationTemplate);
    }

}

