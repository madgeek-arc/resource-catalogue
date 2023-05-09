package eu.einfracentral.registry.service;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstance;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceDto;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ConfigurationTemplateInstanceService<T> extends ResourceService<T, Authentication> {

    List<ConfigurationTemplateInstance> getConfigurationTemplateInstancesByResourceId(String resourceId);
    List<ConfigurationTemplateInstance> getConfigurationTemplateInstancesByConfigurationTemplateId(String configurationTemplateId);
//    ConfigurationTemplateInstanceDto createConfigurationTemplateInstanceDto(ConfigurationTemplateInstance configurationTemplateInstance);
}
