package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
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
