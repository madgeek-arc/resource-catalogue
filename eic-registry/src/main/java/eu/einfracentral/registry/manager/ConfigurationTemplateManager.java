package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
import eu.einfracentral.registry.service.ConfigurationTemplateService;
import org.springframework.security.core.Authentication;

import java.util.UUID;

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

    public ConfigurationTemplateBundle addConfigurationTemplate(ConfigurationTemplateBundle configurationTemplateBundle,
                                                               Authentication auth) {
        configurationTemplateBundle.setId(UUID.randomUUID().toString());
        validate(configurationTemplateBundle);
        super.add(configurationTemplateBundle, auth);
        return configurationTemplateBundle;
    }
}
