package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;

import java.util.List;

public interface ConfigurationTemplateInstanceService extends ResourceService<ConfigurationTemplateInstanceBundle> {

    /**
     * Return a List of ConfigurationTemplateInstances providing a resource ID
     *
     * @param id resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getByResourceId(String id);

    /**
     * Return a List of ConfigurationTemplateInstances providing a ConfigurationTemplate ID
     *
     * @param id resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getByConfigurationTemplateId(String id);

    /**
     * Given a ConfigurationTemplateInstance return a ConfigurationTemplateInstanceDto
     *
     * @param configurationTemplateInstance ConfigurationTemplateInstance
     * @return {@link ConfigurationTemplateInstanceDto}
     */
    ConfigurationTemplateInstanceDto createCTIDto(ConfigurationTemplateInstance configurationTemplateInstance);
}
