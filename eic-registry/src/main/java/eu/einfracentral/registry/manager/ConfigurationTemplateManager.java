package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;

@org.springframework.stereotype.Service("configurationTemplateManager")
public class ConfigurationTemplateManager extends ResourceManager<ConfigurationTemplateBundle> {

    public ConfigurationTemplateManager() {
        super(ConfigurationTemplateBundle.class);
    }

    @Override
    public String getResourceType() {
        return "configuration_template";
    }

}
