package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
import eu.einfracentral.registry.service.ConfigurationTemplateService;

@org.springframework.stereotype.Service("configurationTemplateManager")
public class ConfigurationTemplateManager extends ResourceManager<ConfigurationTemplateBundle>
        implements ConfigurationTemplateService<ConfigurationTemplateBundle> {

    public ConfigurationTemplateManager() {
        super(ConfigurationTemplateBundle.class);
    }

    @Override
    public String getResourceType() {
        return "configuration_template";
    }
}
