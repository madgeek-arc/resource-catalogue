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

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicConfigurationTemplateInstanceManager")
public class PublicConfigurationTemplateInstanceService extends ResourceCatalogueManager<ConfigurationTemplateInstanceBundle>
        implements PublicResourceService<ConfigurationTemplateInstanceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicConfigurationTemplateInstanceService.class);
    private final JmsService jmsService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    public PublicConfigurationTemplateInstanceService(JmsService jmsService,
                                                      InteroperabilityRecordService interoperabilityRecordService,
                                                      ServiceBundleService<ServiceBundle> serviceBundleService,
                                                      TrainingResourceService trainingResourceService) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.jmsService = jmsService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template_instance";
    }

    @Override
    public Browsing<ConfigurationTemplateInstanceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    // TODO: Refactor id, resourceId, payload's interoperabilityRecordId creations when more Catalogues are supported
    @Override
    public ConfigurationTemplateInstanceBundle add(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication authentication) {
        String lowerLevelResourceId = configurationTemplateInstanceBundle.getId();
        configurationTemplateInstanceBundle.setId(configurationTemplateInstanceBundle.getIdentifiers().getPid());
        configurationTemplateInstanceBundle.getMetadata().setPublished(true);

        // set public id to resourceId
        updateIdsToPublic(configurationTemplateInstanceBundle);
        JSONParser parser = new JSONParser();
        try {
            JSONObject payload = (JSONObject) parser.parse(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getPayload().replaceAll("'", "\""));
            //TODO: are CTI from external catalogues supported?
            InteroperabilityRecordBundle bundle = interoperabilityRecordService.get(payload.get("interoperabilityRecordId").toString());
            payload.put("interoperabilityRecordId", bundle.getIdentifiers().getPid());
            configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setPayload(payload.toString());
        } catch (ParseException e) {
            //continue
        }
        ConfigurationTemplateInstanceBundle ret;
        logger.info("ConfigurationTemplateInstanceBundle '{}' is being published with id '{}'",
                lowerLevelResourceId, configurationTemplateInstanceBundle.getId());
        ret = super.add(configurationTemplateInstanceBundle, null);
        jmsService.convertAndSendTopic("configuration_template_instance.create", configurationTemplateInstanceBundle);
        return ret;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication authentication) {
        ConfigurationTemplateInstanceBundle published = super.get(
                configurationTemplateInstanceBundle.getIdentifiers().getPid(),
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getCatalogueId(), true);
        ConfigurationTemplateInstanceBundle ret = super.get(
                configurationTemplateInstanceBundle.getIdentifiers().getPid(),
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, configurationTemplateInstanceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // set public id to resourceId
        updateIdsToPublic(ret);
        ret.setIdentifiers(published.getIdentifiers());
        ret.getConfigurationTemplateInstance().setPayload(published.getConfigurationTemplateInstance().getPayload());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public ConfigurationTemplateInstance with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("configuration_template_instance.update", ret);
        return ret;
    }

    @Override
    public void delete(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        try {
            ConfigurationTemplateInstanceBundle publicConfigurationTemplateInstanceBundle =
                    get(configurationTemplateInstanceBundle.getIdentifiers().getPid());
            logger.info("Deleting public ConfigurationTemplateInstanceBundle with id '{}'",
                    publicConfigurationTemplateInstanceBundle.getId());
            super.delete(publicConfigurationTemplateInstanceBundle);
            jmsService.convertAndSendTopic("configuration_template_instance.delete",
                    publicConfigurationTemplateInstanceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
    public void updateIdsToPublic(ConfigurationTemplateInstanceBundle bundle) {
        // resourceId
        Bundle<?> resourceBundle;
        try {
            resourceBundle = serviceBundleService.get(bundle.getConfigurationTemplateInstance().getResourceId(),
                    bundle.getConfigurationTemplateInstance().getCatalogueId(), false);
        } catch (CatalogueResourceNotFoundException e) {
            resourceBundle = trainingResourceService.get(bundle.getConfigurationTemplateInstance().getResourceId(),
                    bundle.getConfigurationTemplateInstance().getCatalogueId(), false);
        }
        bundle.getConfigurationTemplateInstance().setResourceId(resourceBundle.getIdentifiers().getPid());
    }

}
