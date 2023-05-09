package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstance;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceBundle;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceDto;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ConfigurationTemplateInstanceService;
import eu.einfracentral.registry.service.ConfigurationTemplateService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
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
    private final SecurityService securityService;

    public ConfigurationTemplateInstanceManager(@Lazy ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService,
                                                @Lazy ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService,
                                                @Lazy ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                                SecurityService securityService) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.configurationTemplateService = configurationTemplateService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
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
//        checkResourceIdAndConfigurationTemplateIdConsistency(configurationTemplateInstanceBundle, auth);

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

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the ConfigurationTemplateInstance with id '{}'", auth, configurationTemplateInstanceBundle.getId());

        Resource existing = whereID(configurationTemplateInstanceBundle.getId(), true);
        ConfigurationTemplateInstanceBundle ex = deserialize(existing);
        // check if there are actual changes in the ConfigurationTemplateInstance
        if (configurationTemplateInstanceBundle.getConfigurationTemplateInstance().equals(ex.getConfigurationTemplateInstance())){
            throw new ValidationException("There are no changes in the Configuration Template Instance", HttpStatus.OK);
        }

        // block Public ConfigurationTemplateInstanceBundle updates
        if (configurationTemplateInstanceBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Configuration Template Instance");
        }

        validate(configurationTemplateInstanceBundle);

        configurationTemplateInstanceBundle.setMetadata(Metadata.updateMetadata(configurationTemplateInstanceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey());
        if (configurationTemplateInstanceBundle.getLoggingInfo() != null) {
            loggingInfoList = configurationTemplateInstanceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        configurationTemplateInstanceBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        configurationTemplateInstanceBundle.setLatestUpdateInfo(loggingInfo);

        existing.setPayload(serialize(configurationTemplateInstanceBundle));
        existing.setResourceType(resourceType);

        // block user from updating resourceId
        if (!configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId().equals(ex.getConfigurationTemplateInstance().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Resource Id with which this ConfigurationTemplateInstance is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", configurationTemplateInstanceBundle);

        return configurationTemplateInstanceBundle;
    }

    public void delete(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        // block Public ConfigurationTemplateInstanceBundle deletions
        if (configurationTemplateInstanceBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Configuration Template Instance");
        }
        logger.trace("User is attempting to delete the ConfigurationTemplateInstance with id '{}'",
                configurationTemplateInstanceBundle.getId());
        super.delete(configurationTemplateInstanceBundle);
        logger.debug("Deleting ConfigurationTemplateInstanceBundle: {}", configurationTemplateInstanceBundle);

    }

    public List<ConfigurationTemplateInstance> getConfigurationTemplateInstancesByResourceId(String resourceId){
        List<ConfigurationTemplateInstance> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundles = configurationTemplateInstanceService.getAll(ff, null).getResults();
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundles){
            if (configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId().equals(resourceId)){
                ret.add(configurationTemplateInstanceBundle.getConfigurationTemplateInstance());
            }
        }
        return ret;
    }

    public List<ConfigurationTemplateInstance> getConfigurationTemplateInstancesByConfigurationTemplateId(String configurationTemplateId){
        List<ConfigurationTemplateInstance> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundles = configurationTemplateInstanceService.getAll(ff, null).getResults();
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundles){
            if (configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getConfigurationTemplateId().equals(configurationTemplateId)){
                ret.add(configurationTemplateInstanceBundle.getConfigurationTemplateInstance());
            }
        }
        return ret;
    }

    private void checkResourceIdAndConfigurationTemplateIdConsistency(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth){
        // check if the configuration template ID is related to the resource ID
        boolean found = false;
        FacetFilter ff = new FacetFilter();
        ff.addFilter("published", false);
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

    public ConfigurationTemplateInstanceDto createConfigurationTemplateInstanceDto(ConfigurationTemplateInstance configurationTemplateInstance) {
        ConfigurationTemplateInstanceDto ret = new ConfigurationTemplateInstanceDto();
        ret.setId(configurationTemplateInstance.getId());
        ret.setConfigurationTemplateId(configurationTemplateInstance.getConfigurationTemplateId());
        ret.setResourceId(configurationTemplateInstance.getResourceId());
        JSONParser parser = new JSONParser();
        JSONObject json = null;
        try {
            json = (JSONObject) parser.parse(configurationTemplateInstance.getPayload());
        } catch (ParseException e) {
        }
        ret.setPayload(json);
        return ret;
    }
}