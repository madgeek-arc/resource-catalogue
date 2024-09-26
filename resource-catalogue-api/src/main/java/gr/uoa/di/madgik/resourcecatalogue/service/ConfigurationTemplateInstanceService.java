package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;

import java.util.List;

public interface ConfigurationTemplateInstanceService extends ResourceService<ConfigurationTemplateInstanceBundle> {

    /**
     * Return a List of ConfigurationTemplateInstances providing a resource ID
     *
     * @param resourceId resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getCTIByResourceId(String resourceId);

    /**
     * Return a List of ConfigurationTemplateInstances providing a ConfigurationTemplate ID
     *
     * @param configurationTemplateId resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getCTIByConfigurationTemplateId(
            String configurationTemplateId);

    /**
     * Given a ConfigurationTemplateInstance return a ConfigurationTemplateInstanceDto
     *
     * @param configurationTemplateInstance ConfigurationTemplateInstance
     * @return {@link ConfigurationTemplateInstanceDto}
     */
    ConfigurationTemplateInstanceDto createCTIDto(
            ConfigurationTemplateInstance configurationTemplateInstance);
}
