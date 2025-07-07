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
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicResourceInteroperabilityRecordManager")
public class PublicResourceInteroperabilityRecordService extends ResourceCatalogueManager<ResourceInteroperabilityRecordBundle>
        implements PublicResourceService<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourceInteroperabilityRecordService.class);
    private final JmsService jmsService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    public PublicResourceInteroperabilityRecordService(JmsService jmsService,
                                                       ServiceBundleService<ServiceBundle> serviceBundleService,
                                                       TrainingResourceService trainingResourceService,
                                                       InteroperabilityRecordService interoperabilityRecordService) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.jmsService = jmsService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
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
        resourceInteroperabilityRecordBundle.setId(resourceInteroperabilityRecordBundle.getIdentifiers().getPid());
        resourceInteroperabilityRecordBundle.getMetadata().setPublished(true);

        // sets public ids to resourceId and interoperabilityRecordIds
        updateIdsToPublic(resourceInteroperabilityRecordBundle);

        ResourceInteroperabilityRecordBundle ret;
        logger.info("ResourceInteroperabilityRecordBundle '{}' is being published with id '{}'", lowerLevelResourceId, resourceInteroperabilityRecordBundle.getId());
        ret = super.add(resourceInteroperabilityRecordBundle, null);
        jmsService.convertAndSendTopic("resource_interoperability_record.create", resourceInteroperabilityRecordBundle);
        return ret;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication authentication) {
        ResourceInteroperabilityRecordBundle published = super.get(resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),true);
        ResourceInteroperabilityRecordBundle ret = super.get(resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),true);
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
            ResourceInteroperabilityRecordBundle publicResourceInteroperabilityRecordBundle =
                    get(resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
                            resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(), true);
            logger.info("Deleting public ResourceInteroperabilityRecordBundle with id '{}'", publicResourceInteroperabilityRecordBundle.getId());
            super.delete(publicResourceInteroperabilityRecordBundle);
            jmsService.convertAndSendTopic("resource_interoperability_record.delete", publicResourceInteroperabilityRecordBundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(ResourceInteroperabilityRecordBundle bundle) {
        // resourceId
        Bundle<?> resource;
        try {
            resource = serviceBundleService.get(bundle.getResourceInteroperabilityRecord().getResourceId(),
                    bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
        } catch (CatalogueResourceNotFoundException e) {
            resource = trainingResourceService.get(bundle.getResourceInteroperabilityRecord().getResourceId(),
                    bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
        }
        bundle.getResourceInteroperabilityRecord().setResourceId(resource.getIdentifiers().getPid());

        // Interoperability Record IDs
        List<String> interoperabilityRecordIds = new ArrayList<>();
        for (String interoperabilityRecordId : bundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()) {
            InteroperabilityRecordBundle interoperabilityRecord = interoperabilityRecordService.get(
                    interoperabilityRecordId, bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
            interoperabilityRecordIds.add(interoperabilityRecord.getIdentifiers().getPid());
        }
        bundle.getResourceInteroperabilityRecord().setInteroperabilityRecordIds(interoperabilityRecordIds);
    }
}
