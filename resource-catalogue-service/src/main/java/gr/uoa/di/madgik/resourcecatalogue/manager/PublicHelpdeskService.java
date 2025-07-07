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
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicHelpdeskManager")
public class PublicHelpdeskService extends ResourceCatalogueManager<HelpdeskBundle>
        implements PublicResourceService<HelpdeskBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicHelpdeskService.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public PublicHelpdeskService(JmsService jmsService,
                                 ProviderResourcesCommonMethods commonMethods,
                                 @Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                                 @Lazy TrainingResourceService trainingResourceService) {
        super(HelpdeskBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "helpdesk";
    }

    @Override
    public Browsing<HelpdeskBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    public HelpdeskBundle getOrElseReturnNull(String id, String catalogueId) {
        HelpdeskBundle helpdeskBundle;
        try {
            helpdeskBundle = get(id, catalogueId, true);
        } catch (CatalogueResourceNotFoundException e) {
            return null;
        }
        return helpdeskBundle;
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdeskBundle, Authentication authentication) {
        String lowerLevelResourceId = helpdeskBundle.getId();
        helpdeskBundle.setId(helpdeskBundle.getIdentifiers().getPid());
        helpdeskBundle.getMetadata().setPublished(true);

        // sets public id to serviceId
        updateIdsToPublic(helpdeskBundle);

        HelpdeskBundle ret;
        logger.info("Helpdesk '{}' is being published with id '{}'", lowerLevelResourceId, helpdeskBundle.getId());
        ret = super.add(helpdeskBundle, null);
        jmsService.convertAndSendTopic("helpdesk.create", helpdeskBundle);
        return ret;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdeskBundle, Authentication authentication) {
        HelpdeskBundle published = super.get(helpdeskBundle.getIdentifiers().getPid(), helpdeskBundle.getHelpdesk().getCatalogueId(), true);
        HelpdeskBundle ret = super.get(helpdeskBundle.getIdentifiers().getPid(), helpdeskBundle.getHelpdesk().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, helpdeskBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public id to serviceId
        updateIdsToPublic(helpdeskBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Helpdesk with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("helpdesk.update", helpdeskBundle);
        return ret;
    }

    @Override
    public void delete(HelpdeskBundle helpdeskBundle) {
        try {
            HelpdeskBundle publicHelpdeskBundle = get(helpdeskBundle.getIdentifiers().getPid(),
                    helpdeskBundle.getHelpdesk().getCatalogueId(), true);
            logger.info("Deleting public Helpdesk with id '{}'", publicHelpdeskBundle.getId());
            super.delete(publicHelpdeskBundle);
            jmsService.convertAndSendTopic("helpdesk.delete", publicHelpdeskBundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(HelpdeskBundle bundle) {
        // serviceId
        Bundle<?> resourceBundle;
        try {
            resourceBundle = serviceBundleService.get(bundle.getHelpdesk().getServiceId(), bundle.getHelpdesk().getCatalogueId(), false);
        } catch (CatalogueResourceNotFoundException e) {
            resourceBundle = trainingResourceService.get(bundle.getHelpdesk().getServiceId(), bundle.getHelpdesk().getCatalogueId(), false);
        }
        bundle.getHelpdesk().setServiceId(resourceBundle.getIdentifiers().getPid());
    }
}
