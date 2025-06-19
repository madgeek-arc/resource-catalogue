/**
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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.util.*;

@org.springframework.stereotype.Service("configurationTemplateManager")
public class ConfigurationTemplateManager extends ResourceCatalogueManager<ConfigurationTemplateBundle>
        implements ConfigurationTemplateService {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateManager.class);
    private final IdCreator idCreator;
    private final ProviderResourcesCommonMethods commonMethods;
    private final ProviderService providerService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final PublicConfigurationTemplateService publicConfigurationTemplateService;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ConfigurationTemplateManager(IdCreator idCreator, ProviderResourcesCommonMethods commonMethods,
                                        ProviderService providerService, InteroperabilityRecordService interoperabilityRecordService,
                                        PublicConfigurationTemplateService publicConfigurationTemplateService,
                                        GenericResourceService genericResourceService) {
        super(ConfigurationTemplateBundle.class);
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.providerService = providerService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.publicConfigurationTemplateService = publicConfigurationTemplateService;
        this.genericResourceService = genericResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template";
    }

    @Override
    public ConfigurationTemplateBundle add(ConfigurationTemplateBundle bundle, Authentication auth) {
        return add(bundle, bundle.getConfigurationTemplate().getCatalogueId(), auth);
    }

    @Override
    public ConfigurationTemplateBundle add(ConfigurationTemplateBundle bundle, String catalogueId, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(
                bundle.getConfigurationTemplate().getInteroperabilityRecordId(), catalogueId, false);
        if (!interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId().equals(catalogueId)) {
            throw new ValidationException(String.format("There is no Interoperability Record with ID %s in the %s Catalogue.",
                    interoperabilityRecordBundle.getId(), catalogueId));
        }

        bundle.setId(idCreator.generate(getResourceTypeName()));
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);

        ProviderBundle providerBundle = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ResourceException(String.format("The Provider ID '%s' you provided is not yet approved",
                    providerBundle.getId()), HttpStatus.CONFLICT);
        }
        validate(bundle);

        if (bundle.getMetadata() == null) {
            bundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth)));
        }
        // loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        bundle.setLatestOnboardingInfo(loggingInfoList.getFirst());
        bundle.setActive(true);
        LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.APPROVED.getKey());
        loggingInfoList.add(loggingInfoApproved);
        bundle.setLatestOnboardingInfo(loggingInfoApproved);
        bundle.setLoggingInfo(loggingInfoList);

        super.add(bundle, auth);
        logger.info("Added a new Configuration Template with id '{}' and title '{}'", bundle.getId(),
                bundle.getConfigurationTemplate().getName());
        return bundle;
    }

    @Override
    public ConfigurationTemplateBundle update(ConfigurationTemplateBundle bundle, Authentication auth) {
        return update(bundle, bundle.getConfigurationTemplate().getCatalogueId(), auth);
    }

    @Override
    public ConfigurationTemplateBundle update(ConfigurationTemplateBundle bundle, String catalogueId, Authentication auth) {
        ConfigurationTemplateBundle ret = ObjectUtils.clone(bundle);
        ConfigurationTemplateBundle existingConfigurationTemplate;
        existingConfigurationTemplate = get(ret.getConfigurationTemplate().getId(), catalogueId, false);
        if (ret.getConfigurationTemplate().equals(existingConfigurationTemplate.getConfigurationTemplate())) {
            return ret;
        }

        if (catalogueId == null || catalogueId.isEmpty()) {
            ret.getConfigurationTemplate().setCatalogueId(this.catalogueId);
        }

        validate(ret);

        // block Public Configuration Template update
        if (existingConfigurationTemplate.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Configuration Template");
        }

        // update existing ConfigurationTemplate Metadata, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingConfigurationTemplate.getMetadata(), AuthenticationInfo.getFullName(auth)));
        ret.setIdentifiers(existingConfigurationTemplate.getIdentifiers());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingConfigurationTemplate, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));

        // active/status
        ret.setActive(existingConfigurationTemplate.isActive());
        ret.setSuspended(existingConfigurationTemplate.isSuspended());

        Resource existing = getResource(ret.getConfigurationTemplate().getId(), catalogueId, false);
        existing.setPayload(serialize(ret));
        existing.setResourceType(getResourceType());

        resourceService.updateResource(existing);
        logger.info("Updated Configuration Template with id '{}' and title '{}'", ret.getId(),
                ret.getConfigurationTemplate().getName());

        return ret;
    }

    @Override
    public void delete(ConfigurationTemplateBundle bundle) {
        // block Public ConfigurationTemplate deletions
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Configuration Template");
        }
        super.delete(bundle);
        logger.info("Deleted the Configuration Template with id '{}'", bundle.getId());
    }

    @Override
    public Paging<ConfigurationTemplate> getAllByInteroperabilityRecordId(MultiValueMap<String, Object> allRequestParams,
                                                                          String interoperabilityRecordId) {
        FacetFilter ff;
        if (allRequestParams != null) {
            ff = FacetFilter.from(allRequestParams);
        } else {
            ff = new FacetFilter();
        }
        ff.setResourceType("configuration_template");
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        ff.addFilter("interoperability_record_id", interoperabilityRecordId);
        return genericResourceService.getResults(ff);
    }

    public Map<String, List<String>> getInteroperabilityRecordIdToConfigurationTemplateListMap() {
        Map<String, List<String>> ret = new HashMap<>();
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(1000);
        filter.addFilter("published", false);
        List<ConfigurationTemplateBundle> ctList = getAll(filter).getResults();

        for (ConfigurationTemplateBundle ctBundle : ctList) {
            String igId = ctBundle.getConfigurationTemplate().getInteroperabilityRecordId();
            String ctId = ctBundle.getId();

            ret.computeIfAbsent(igId, k -> new ArrayList<>()).add(ctId);
        }
        return ret;
    }

    public ConfigurationTemplateBundle createPublicConfigurationTemplate(ConfigurationTemplateBundle bundle, Authentication auth) {
        publicConfigurationTemplateService.add(bundle, auth);
        return bundle;
    }
}
