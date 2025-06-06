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
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicConfigurationTemplateInstanceManager")
public class PublicConfigurationTemplateInstanceService extends ResourceManager<ConfigurationTemplateInstanceBundle>
        implements PublicResourceService<ConfigurationTemplateInstanceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicConfigurationTemplateInstanceService.class);
    private final JmsService jmsService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public PublicConfigurationTemplateInstanceService(JmsService jmsService) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.jmsService = jmsService;
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
        Identifiers.createOriginalId(configurationTemplateInstanceBundle);
        configurationTemplateInstanceBundle.setId(PublicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));

        // set public id to resourceId
        updateIdsToPublic(configurationTemplateInstanceBundle);
        JSONParser parser = new JSONParser();
        try {
            JSONObject payload = (JSONObject) parser.parse(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getPayload().replaceAll("'", "\""));
            payload.put("interoperabilityRecordId", PublicResourceUtils.createPublicResourceId(
                    payload.get("interoperabilityRecordId").toString(), catalogueId));
            configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setPayload(payload.toString());
        } catch (ParseException e) {
            //continue
        }
        configurationTemplateInstanceBundle.getMetadata().setPublished(true);
        ConfigurationTemplateInstanceBundle ret;
        logger.info("ConfigurationTemplateInstanceBundle '{}' is being published with id '{}'",
                lowerLevelResourceId, configurationTemplateInstanceBundle.getId());
        ret = super.add(configurationTemplateInstanceBundle, null);
        jmsService.convertAndSendTopic("configuration_template_instance.create", configurationTemplateInstanceBundle);
        return ret;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication authentication) {
        ConfigurationTemplateInstanceBundle published = super.get(PublicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));
        ConfigurationTemplateInstanceBundle ret = super.get(PublicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));
        try {
            BeanUtils.copyProperties(ret, configurationTemplateInstanceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // set public id to resourceId
        updateIdsToPublic(ret);
        ret.setIdentifiers(published.getIdentifiers());
        ret.getConfigurationTemplateInstance().setPayload(published.getConfigurationTemplateInstance().getPayload()); //TODO: refactor when users will be able to update CTIs
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public ResourceInteroperabilityRecordBundle with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("resource_interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        try {
            ConfigurationTemplateInstanceBundle publicConfigurationTemplateInstanceBundle =
                    get(PublicResourceUtils.createPublicResourceId(
                    configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));
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
        bundle.getConfigurationTemplateInstance().setResourceId(PublicResourceUtils.createPublicResourceId(
                bundle.getConfigurationTemplateInstance().getResourceId(), catalogueId));
        //TODO: enable if we have public CT
        // configurationTemplateId
//        configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setResourceId(PublicResourceUtils.createPublicResourceId(
//                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getConfigurationTemplateId(), catalogueId));
    }

}
