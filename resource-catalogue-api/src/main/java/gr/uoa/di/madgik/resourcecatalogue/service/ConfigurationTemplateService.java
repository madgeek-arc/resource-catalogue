package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import org.springframework.security.core.Authentication;

public interface ConfigurationTemplateService<T> extends ResourceService<T, Authentication> {

    /**
     * Add a new Configuration Template.
     *
     * @param configurationTemplateBundle ConfigurationTemplateBundle
     * @param auth                        Authentication
     * @return {@link ConfigurationTemplateBundle}
     */
    ConfigurationTemplateBundle addConfigurationTemplate(ConfigurationTemplateBundle configurationTemplateBundle,
                                                         Authentication auth);
}
