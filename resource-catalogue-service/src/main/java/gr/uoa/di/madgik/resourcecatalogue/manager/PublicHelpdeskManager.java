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
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicHelpdeskManager")
public class PublicHelpdeskManager extends AbstractPublicResourceManager<HelpdeskBundle> implements ResourceCRUDService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicHelpdeskManager.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicResourceUtils publicResourceUtils;

    public PublicHelpdeskManager(JmsService jmsService,
                                 ProviderResourcesCommonMethods commonMethods,
                                 PublicResourceUtils publicResourceUtils) {
        super(HelpdeskBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.publicResourceUtils = publicResourceUtils;
    }

    @Override
    public String getResourceTypeName() {
        return "helpdesk";
    }

    @Override
    public Browsing<HelpdeskBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    public HelpdeskBundle getOrElseReturnNull(String id) {
        HelpdeskBundle helpdeskBundle;
        try {
            helpdeskBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return helpdeskBundle;
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdeskBundle, Authentication authentication) {
        String lowerLevelResourceId = helpdeskBundle.getId();
        Identifiers.createOriginalId(helpdeskBundle);
        helpdeskBundle.setId(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                helpdeskBundle.getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(helpdeskBundle.getId(), helpdeskBundle.getCatalogueId());

        // sets public id to serviceId
        updateHelpdeskIdsToPublic(helpdeskBundle);

        helpdeskBundle.getMetadata().setPublished(true);
        HelpdeskBundle ret;
        logger.info("Helpdesk '{}' is being published with id '{}'", lowerLevelResourceId, helpdeskBundle.getId());
        ret = super.add(helpdeskBundle, null);
        jmsService.convertAndSendTopic("helpdesk.create", helpdeskBundle);
        return ret;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdeskBundle, Authentication authentication) {
        HelpdeskBundle published = super.get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                helpdeskBundle.getCatalogueId()));
        HelpdeskBundle ret = super.get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                helpdeskBundle.getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, helpdeskBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public id to serviceId
        updateHelpdeskIdsToPublic(helpdeskBundle);

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
            HelpdeskBundle publicHelpdeskBundle = get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                    helpdeskBundle.getCatalogueId()));
            logger.info("Deleting public Helpdesk with id '{}'", publicHelpdeskBundle.getId());
            super.delete(publicHelpdeskBundle);
            jmsService.convertAndSendTopic("helpdesk.delete", publicHelpdeskBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
