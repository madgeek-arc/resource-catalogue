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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicResourceInteroperabilityRecordManager")
public class PublicResourceInteroperabilityRecordService extends ResourceManager<ResourceInteroperabilityRecordBundle>
        implements PublicResourceService<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourceInteroperabilityRecordService.class);
    private final JmsService jmsService;

    public PublicResourceInteroperabilityRecordService(JmsService jmsService) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.jmsService = jmsService;
    }

    @Override
    public String getResourceTypeName() {
        return "resource_interoperability_record";
    }

    @Override
    public Browsing<ResourceInteroperabilityRecordBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication authentication) {
        String lowerLevelResourceId = resourceInteroperabilityRecordBundle.getId();
        Identifiers.createOriginalId(resourceInteroperabilityRecordBundle);
        resourceInteroperabilityRecordBundle.setId(PublicResourceUtils.createPublicResourceId(
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));

        // sets public ids to resourceId and interoperabilityRecordIds
        updateIdsToPublic(resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.getMetadata().setPublished(true);
        ResourceInteroperabilityRecordBundle ret;
        logger.info("ResourceInteroperabilityRecordBundle '{}' is being published with id '{}'", lowerLevelResourceId, resourceInteroperabilityRecordBundle.getId());
        ret = super.add(resourceInteroperabilityRecordBundle, null);
        jmsService.convertAndSendTopic("resource_interoperability_record.create", resourceInteroperabilityRecordBundle);
        return ret;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication authentication) {
        ResourceInteroperabilityRecordBundle published = super.get(PublicResourceUtils.createPublicResourceId(
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
        ResourceInteroperabilityRecordBundle ret = super.get(PublicResourceUtils.createPublicResourceId(
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, resourceInteroperabilityRecordBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to resourceId and interoperabilityRecordIds
        updateIdsToPublic(ret);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public ResourceInteroperabilityRecordBundle with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("resource_interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        try {
            ResourceInteroperabilityRecordBundle publicResourceInteroperabilityRecordBundle = get(PublicResourceUtils.createPublicResourceId(
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
            logger.info("Deleting public ResourceInteroperabilityRecordBundle with id '{}'", publicResourceInteroperabilityRecordBundle.getId());
            super.delete(publicResourceInteroperabilityRecordBundle);
            jmsService.convertAndSendTopic("resource_interoperability_record.delete", publicResourceInteroperabilityRecordBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(ResourceInteroperabilityRecordBundle bundle) {
        // resourceId
        bundle.getResourceInteroperabilityRecord().setResourceId(PublicResourceUtils.createPublicResourceId(
                bundle.getResourceInteroperabilityRecord().getResourceId(),
                bundle.getResourceInteroperabilityRecord().getCatalogueId()));
        // Interoperability Record IDs
        bundle.getResourceInteroperabilityRecord().setInteroperabilityRecordIds(
                appendCatalogueId(
                        bundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds(),
                        bundle.getResourceInteroperabilityRecord().getCatalogueId()));
    }
}
