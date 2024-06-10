package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import org.springframework.security.core.Authentication;

@org.springframework.stereotype.Service("configurationTemplateManager")
public class ConfigurationTemplateManager extends ResourceManager<ConfigurationTemplateBundle>
        implements ConfigurationTemplateService {

    private final IdCreator idCreator;

    public ConfigurationTemplateManager(IdCreator idCreator) {
        super(ConfigurationTemplateBundle.class);
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceType() {
        return "configuration_template";
    }

    public ConfigurationTemplateBundle addConfigurationTemplate(ConfigurationTemplateBundle configurationTemplateBundle,
                                                                Authentication auth) {
        configurationTemplateBundle.setId(idCreator.generate(getResourceType()));
        validate(configurationTemplateBundle);
        super.add(configurationTemplateBundle, auth);
        return configurationTemplateBundle;
    }
}
