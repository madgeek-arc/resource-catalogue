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

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicConfigurationTemplateManager")
public class PublicConfigurationTemplateService extends ResourceCatalogueManager<ConfigurationTemplateBundle>
        implements PublicResourceService<ConfigurationTemplateBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicConfigurationTemplateService.class);
    private final JmsService jmsService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public PublicConfigurationTemplateService(JmsService jmsService,
                                              InteroperabilityRecordService interoperabilityRecordService) {
        super(ConfigurationTemplateBundle.class);
        this.jmsService = jmsService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template";
    }

    @Override
    public Browsing<ConfigurationTemplateBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public ConfigurationTemplateBundle add(ConfigurationTemplateBundle configurationTemplateBundle, Authentication authentication) {
        String lowerLevelResourceId = configurationTemplateBundle.getId();
        configurationTemplateBundle.setId(configurationTemplateBundle.getIdentifiers().getPid());
        configurationTemplateBundle.getMetadata().setPublished(true);

        updateIdsToPublic(configurationTemplateBundle);

        ConfigurationTemplateBundle ret;
        logger.info("ConfigurationTemplateBundle '{}' is being published with id '{}'",
                lowerLevelResourceId, configurationTemplateBundle.getId());
        ret = super.add(configurationTemplateBundle, null);
        jmsService.convertAndSendTopic("configuration_template.create", configurationTemplateBundle);
        return ret;
    }

    @Override
    public ConfigurationTemplateBundle update(ConfigurationTemplateBundle configurationTemplateBundle, Authentication authentication) {
        ConfigurationTemplateBundle published = super.get(configurationTemplateBundle.getIdentifiers().getPid(),
                configurationTemplateBundle.getConfigurationTemplate().getCatalogueId(), true);
        ConfigurationTemplateBundle ret = super.get(configurationTemplateBundle.getIdentifiers().getPid(),
                configurationTemplateBundle.getConfigurationTemplate().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, configurationTemplateBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // set public id to resourceId
        updateIdsToPublic(ret);
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public ConfigurationTemplate with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("configuration_template.update", ret);
        return ret;
    }

    @Override
    public void delete(ConfigurationTemplateBundle configurationTemplateBundle) {
        try {
            ConfigurationTemplateBundle publicConfigurationTemplateBundle =
                    get(configurationTemplateBundle.getIdentifiers().getPid(),
                            configurationTemplateBundle.getConfigurationTemplate().getCatalogueId(), true);
            logger.info("Deleting public ConfigurationTemplateBundle with id '{}'",
                    publicConfigurationTemplateBundle.getId());
            super.delete(publicConfigurationTemplateBundle);
            jmsService.convertAndSendTopic("configuration_template.delete",
                    publicConfigurationTemplateBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    public void updateIdsToPublic(ConfigurationTemplateBundle bundle) {
        // resourceId
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(
                bundle.getConfigurationTemplate().getInteroperabilityRecordId(), bundle.getConfigurationTemplate().getCatalogueId(), false);
        bundle.getConfigurationTemplate().setInteroperabilityRecordId(interoperabilityRecordBundle.getIdentifiers().getPid());
    }

}
