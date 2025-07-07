/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@org.springframework.stereotype.Service("configurationTemplateInstanceManager")
public class ConfigurationTemplateInstanceManager extends ResourceCatalogueManager<ConfigurationTemplateInstanceBundle>
        implements ConfigurationTemplateInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateInstanceManager.class);
    private final ConfigurationTemplateInstanceService configInstanceService;
    private final ResourceInteroperabilityRecordService rirService;

    private final ConfigurationTemplateService configService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;
    private final PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceService;

    public ConfigurationTemplateInstanceManager(@Lazy ConfigurationTemplateInstanceService configInstanceService,
                                                @Lazy ConfigurationTemplateService configService,
                                                @Lazy ResourceInteroperabilityRecordService rirService,
                                                SecurityService securityService, IdCreator idCreator,
                                                ProviderResourcesCommonMethods commonMethods,
                                                PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceService) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.configInstanceService = configInstanceService;
        this.configService = configService;
        this.rirService = rirService;
        this.securityService = securityService;
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.publicConfigurationTemplateInstanceService = publicConfigurationTemplateInstanceService;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template_instance";
    }

    @Override
    public ConfigurationTemplateInstanceBundle add(ConfigurationTemplateInstanceBundle bundle, Authentication auth) {
        validate(bundle);
        checkResourceIdAndConfigurationTemplateIdConsistency(bundle, auth);
        validateInstanceAgainstTemplate(bundle);

        bundle.setId(idCreator.generate(getResourceTypeName()));
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);
        logger.trace("Attempting to add a new ConfigurationTemplateInstance: {}", bundle);

        bundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase()));
        List<LoggingInfo> list = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        bundle.setLoggingInfo(list);
        bundle.setLatestOnboardingInfo(list.getFirst());

        // active
        bundle.setActive(true);

        ConfigurationTemplateInstanceBundle ret;
        ret = super.add(bundle, null);
        logger.info("Added Configuration Template Instance with id '{}'", ret.getId());

        return ret;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle bundle, Authentication auth) {
        logger.trace("Attempting to update the ConfigurationTemplateInstance with id '{}'", bundle.getId());

        ConfigurationTemplateInstanceBundle ret = ObjectUtils.clone(bundle);
        Resource existing = getResource(ret.getConfigurationTemplateInstance().getId(),
                ret.getConfigurationTemplateInstance().getCatalogueId(), false);
        ConfigurationTemplateInstanceBundle existingCTI = deserialize(existing);
        // check if there are actual changes in the ConfigurationTemplateInstance
        if (ret.getConfigurationTemplateInstance().equals(existingCTI.getConfigurationTemplateInstance())) {
            return ret;
        }

        // block Public ConfigurationTemplateInstanceBundle updates
        if (ret.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Configuration Template Instance");
        }

        validate(ret);

        ret.setMetadata(Metadata.updateMetadata(
                ret.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        ret.setIdentifiers(existingCTI.getIdentifiers());
        List<LoggingInfo> list = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingCTI, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        list.add(loggingInfo);
        list.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(list);

        // latestUpdateInfo
        ret.setLatestUpdateInfo(loggingInfo);

        existing.setPayload(serialize(ret));
        existing.setResourceType(getResourceType());

        // block user from updating resourceId
        if (!ret.getConfigurationTemplateInstance().getResourceId().equals(existingCTI.getConfigurationTemplateInstance().getResourceId()) &&
                !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Resource Id with which this " +
                    "ConfigurationTemplateInstance is related");
        }

        // block user from updating configurationTemplateId
        if (!ret.getConfigurationTemplateInstance().getConfigurationTemplateId()
                .equals(existingCTI.getConfigurationTemplateInstance().getConfigurationTemplateId()) &&
                !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Configuration Template Id with which this " +
                    "ConfigurationTemplateInstance is related");
        }

        resourceService.updateResource(existing);
        logger.info("Updated Configuration Template Instance with id '{}'", bundle.getId());

        return ret;
    }

    @Override
    public void delete(ConfigurationTemplateInstanceBundle bundle) {
        // block Public ConfigurationTemplateInstanceBundle deletions
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Configuration Template Instance");
        }
        logger.trace("User is attempting to delete the ConfigurationTemplateInstance with id '{}'",
                bundle.getId());
        super.delete(bundle);
        logger.info("Deleted the Configuration Template Instance with id '{}'", bundle.getConfigurationTemplateInstance().getId());
    }

    public List<ConfigurationTemplateInstanceBundle> getByResourceId(String id) {
        List<ConfigurationTemplateInstanceBundle> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ConfigurationTemplateInstanceBundle> list = configInstanceService.getAll(ff, null).getResults();
        for (ConfigurationTemplateInstanceBundle bundle : list) {
            if (bundle.getConfigurationTemplateInstance().getResourceId().equals(id)) {
                ret.add(bundle);
            }
        }
        return ret;
    }

    public List<ConfigurationTemplateInstance> getByConfigurationTemplateId(String id) {
        List<ConfigurationTemplateInstance> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        List<ConfigurationTemplateInstanceBundle> list = configInstanceService.getAll(ff, null).getResults();
        for (ConfigurationTemplateInstanceBundle bundle : list) {
            if (bundle.getConfigurationTemplateInstance().getConfigurationTemplateId().equals(id)) {
                ret.add(bundle.getConfigurationTemplateInstance());
            }
        }
        return ret;
    }

    public ConfigurationTemplateInstance getByResourceAndConfigurationTemplateId(String resourceId, String ctId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        ff.addFilter("resource_id", resourceId);
        ff.addFilter("configuration_template_id", ctId);
        List<ConfigurationTemplateInstanceBundle> list = configInstanceService.getAll(ff, null).getResults();
        if (!list.isEmpty()) {
            return list.getFirst().getConfigurationTemplateInstance();
        }
        return null;
    }

    private void checkResourceIdAndConfigurationTemplateIdConsistency(ConfigurationTemplateInstanceBundle bundle,
                                                                      Authentication auth) {
        String resourceId = bundle.getConfigurationTemplateInstance().getResourceId();
        String configurationTemplateId = bundle.getConfigurationTemplateInstance().getConfigurationTemplateId();
        // check if the configuration template ID is related to the resource ID
        boolean found = false;
        List<ResourceInteroperabilityRecordBundle> list = rirService.getAll(createFacetFilter(), auth).getResults();
        for (ResourceInteroperabilityRecordBundle rirBundle : list) {
            if (rirBundle.getResourceInteroperabilityRecord().getResourceId().equals(resourceId)) {
                ConfigurationTemplateBundle ctBundle = configService.get(configurationTemplateId);
                if (rirBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()
                        .contains(ctBundle.getConfigurationTemplate().getInteroperabilityRecordId())) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            throw new ValidationException("Fields resourceId and configurationTemplateId are not related.");
        }

        // check if a Configuration Template Implementation with the same resourceId, configurationTemplateId already exists
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundleList = configInstanceService.getAll(createFacetFilter(), auth).getResults();
        for (ConfigurationTemplateInstanceBundle ctiBundle : configurationTemplateInstanceBundleList) {
            if (ctiBundle.getConfigurationTemplateInstance().getResourceId().equals(resourceId) &&
                    ctiBundle.getConfigurationTemplateInstance().getConfigurationTemplateId().equals(configurationTemplateId)) {
                throw new ValidationException(String.format("There is already a Configuration Template Instance registered " +
                                "for Resource [%s] under [%s] Configuration Template",
                        resourceId, configurationTemplateId));
            }
        }
    }

    private FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        return ff;
    }

    private void validateInstanceAgainstTemplate(ConfigurationTemplateInstanceBundle bundle) {
        ConfigurationTemplateBundle ct = configService.get(bundle.getConfigurationTemplateInstance().getConfigurationTemplateId(),
                bundle.getConfigurationTemplateInstance().getCatalogueId(), false);
        Set<String> ctKeys = ct.getConfigurationTemplate().getFormModel().keySet();
        Set<String> ctiKeys = bundle.getConfigurationTemplateInstance().getPayload().keySet();

        if (!ctKeys.equals(ctiKeys)) {
            throw new ValidationException("Configuration Template Instance does not contain the required model in its payload");
        }
    }

    public ConfigurationTemplateInstanceBundle createPublicConfigurationTemplateInstance(
            ConfigurationTemplateInstanceBundle bundle, Authentication auth) {
        publicConfigurationTemplateInstanceService.add(bundle, auth);
        return bundle;
    }
}
