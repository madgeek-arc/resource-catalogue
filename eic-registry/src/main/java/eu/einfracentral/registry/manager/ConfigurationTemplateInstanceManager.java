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
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final ProviderResourcesCommonMethods commonMethods;

    public ConfigurationTemplateInstanceManager(@Lazy ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService,
                                                @Lazy ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService,
                                                @Lazy ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                                SecurityService securityService, ProviderResourcesCommonMethods commonMethods) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.configurationTemplateService = configurationTemplateService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
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
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(configurationTemplateInstanceBundle, auth);
        configurationTemplateInstanceBundle.setLoggingInfo(loggingInfoList);
        configurationTemplateInstanceBundle.setLatestOnboardingInfo(loggingInfoList.get(0));

        // active
        configurationTemplateInstanceBundle.setActive(true);

        ConfigurationTemplateInstanceBundle ret;
        ret = super.add(configurationTemplateInstanceBundle, null);
        logger.debug("Adding ConfigurationTemplateInstanceBundle: {}", configurationTemplateInstanceBundle);

        return ret;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the ConfigurationTemplateInstance with id '{}'", auth, configurationTemplateInstanceBundle.getId());

        ConfigurationTemplateInstanceBundle ret = ObjectUtils.clone(configurationTemplateInstanceBundle);
        Resource existingResource = whereID(ret.getId(), true);
        ConfigurationTemplateInstanceBundle existingCTI = deserialize(existingResource);
        // check if there are actual changes in the ConfigurationTemplateInstance
        if (ret.getConfigurationTemplateInstance().equals(existingCTI.getConfigurationTemplateInstance())) {
            return ret;
        }

        // block Public ConfigurationTemplateInstanceBundle updates
        if (ret.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Configuration Template Instance");
        }

        validate(ret);

        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingCTI, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        ret.setLatestUpdateInfo(loggingInfo);

        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);

        // block user from updating resourceId
        if (!ret.getConfigurationTemplateInstance().getResourceId().equals(existingCTI.getConfigurationTemplateInstance().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Resource Id with which this ConfigurationTemplateInstance is related");
        }

        // block user from updating configurationTemplateId
        if (!ret.getConfigurationTemplateInstance().getConfigurationTemplateId().equals(existingCTI.getConfigurationTemplateInstance().getConfigurationTemplateId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Configuration Template Id with which this ConfigurationTemplateInstance is related");
        }

        resourceService.updateResource(existingResource);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", ret);

        return ret;
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
        String resourceId = configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId();
        String configurationTemplateId = configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getConfigurationTemplateId();
        // check if the configuration template ID is related to the resource ID
        boolean found = false;
        List<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundleList = resourceInteroperabilityRecordService.getAll(createFacetFilter(), auth).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : resourceInteroperabilityRecordBundleList){
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId().equals(resourceId)){
                ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.get(configurationTemplateId);
                if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds().contains(configurationTemplateBundle.getConfigurationTemplate().getInteroperabilityRecordId())){
                    found = true;
                    break;
                }
            }
        }
        if (!found){
            throw new ValidationException("Fields resourceId and configurationTemplateId are not related.");
        }

        // check if a Configuration Template Implementation with the same resourceId, configurationTemplateId and payload already exists
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundleList = configurationTemplateInstanceService.getAll(createFacetFilter(), auth).getResults();
        for (ConfigurationTemplateInstanceBundle ctiBundle : configurationTemplateInstanceBundleList){
            if (ctiBundle.getConfigurationTemplateInstance().getResourceId().equals(resourceId) &&
                ctiBundle.getConfigurationTemplateInstance().getConfigurationTemplateId().equals(configurationTemplateId) &&
                ctiBundle.getConfigurationTemplateInstance().getPayload().equals(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getPayload())){
                throw new ValidationException(String.format("There is already a Configuration Template Instance registered for Resource [%s] under [%s] Configuration Template with the same payload",
                        resourceId, configurationTemplateId));
            }
        }
    }

    private FacetFilter createFacetFilter(){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        return ff;
    }

    public ConfigurationTemplateInstanceDto createConfigurationTemplateInstanceDto(ConfigurationTemplateInstance configurationTemplateInstance) {
        ConfigurationTemplateInstanceDto ret = new ConfigurationTemplateInstanceDto();
        ret.setId(configurationTemplateInstance.getId());
        ret.setConfigurationTemplateId(configurationTemplateInstance.getConfigurationTemplateId());
        ret.setResourceId(configurationTemplateInstance.getResourceId());
        JSONParser parser = new JSONParser();
        try {
            String jsonString = configurationTemplateInstance.getPayload();
            jsonString = jsonString.replace("'", "\"");
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            ret.setPayload(jsonObject);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
