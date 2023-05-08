package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplate;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ConfigurationTemplateInstanceService;
import eu.einfracentral.registry.service.ConfigurationTemplateService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("configurationTemplateInstanceManager")
public class ConfigurationTemplateInstanceManager extends ResourceManager<ConfigurationTemplateInstanceBundle>
        implements ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateInstanceManager.class);
    private final ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;

    private final ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final SecurityService securityService;

    public ConfigurationTemplateInstanceManager(ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService,
                                                ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService,
                                                ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                                ResourceBundleService<ServiceBundle> resourceBundleService, SecurityService securityService) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.configurationTemplateService = configurationTemplateService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.resourceBundleService = resourceBundleService;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "configuration_template_instance";
    }

    // TODO: validate/add/update/delete

    @Override
    public ConfigurationTemplateInstanceBundle add(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth) {
        validate(configurationTemplateInstanceBundle);
        checkResourceIdAndConfigurationTemplateIdConsistency(configurationTemplateInstanceBundle, auth);

        configurationTemplateInstanceBundle.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new ConfigurationTemplateInstance: {}", auth, configurationTemplateInstanceBundle);

        configurationTemplateInstanceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        configurationTemplateInstanceBundle.setLoggingInfo(loggingInfoList);
        configurationTemplateInstanceBundle.setActive(true);

        // latestOnboardingInfo
        configurationTemplateInstanceBundle.setLatestOnboardingInfo(loggingInfo);

        ConfigurationTemplateInstanceBundle ret;
        ret = super.add(configurationTemplateInstanceBundle, null);
        logger.debug("Adding ConfigurationTemplateInstanceBundle: {}", configurationTemplateInstanceBundle);

        return ret;
    }

    public ConfigurationTemplateInstance getConfigurationTemplateInstanceByResourceId(String resourceId, Authentication auth){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundles = configurationTemplateInstanceService.getAll(ff, auth).getResults();
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundles){
            if (configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId().equals(resourceId)){
                return configurationTemplateInstanceBundle.getConfigurationTemplateInstance();
            }
        }
        return null;
    }

    public ConfigurationTemplateInstance getConfigurationTemplateInstanceByConfigurationTemplateId(String configurationTemplateId, Authentication auth){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundles = configurationTemplateInstanceService.getAll(ff, auth).getResults();
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundles){
            if (configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId().equals(configurationTemplateId)){
                return configurationTemplateInstanceBundle.getConfigurationTemplateInstance();
            }
        }
        return null;
    }

    private void checkResourceIdAndConfigurationTemplateIdConsistency(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth){
        // check if the configuration template ID is related to the resource ID
        boolean found = false;
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundleList = resourceInteroperabilityRecordService.getAll(ff, auth).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : resourceInteroperabilityRecordBundleList){
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId().
                    equals(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId())){
                ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.get(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getConfigurationTemplateId());
                if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds().contains(configurationTemplateBundle.getConfigurationTemplate().getInteroperabilityRecordId())){
                    found = true;
                    break;
                }
            }
        }
        if (!found){
            throw new ValidationException("Fields resourceId and configurationTemplateId are not related.");
        }
    }

}
