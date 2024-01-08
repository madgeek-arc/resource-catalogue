package eu.einfracentral.registry.service;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstance;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceDto;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ConfigurationTemplateInstanceService<T> extends ResourceService<T, Authentication> {

    /**
     * Return a List of ConfigurationTemplateInstances providing a resource ID
     *
     * @param resourceId The ID of the resource
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getConfigurationTemplateInstancesByResourceId(String resourceId);

    /**
     * Return a List of ConfigurationTemplateInstances providing a ConfigurationTemplate ID
     *
     * @param configurationTemplateId The ID of the resource
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getConfigurationTemplateInstancesByConfigurationTemplateId(
            String configurationTemplateId);

    /**
     * Given a ConfigurationTemplateInstance return a ConfigurationTemplateInstanceDto
     *
     * @param configurationTemplateInstance ConfigurationTemplateInstance
     * @return {@link ConfigurationTemplateInstanceDto}
     */
    ConfigurationTemplateInstanceDto createConfigurationTemplateInstanceDto(
            ConfigurationTemplateInstance configurationTemplateInstance);
}
