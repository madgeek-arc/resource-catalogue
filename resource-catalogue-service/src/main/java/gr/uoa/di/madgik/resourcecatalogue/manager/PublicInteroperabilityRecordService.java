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
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicInteroperabilityRecordManager")
public class PublicInteroperabilityRecordService extends ResourceCatalogueManager<InteroperabilityRecordBundle>
        implements PublicResourceService<InteroperabilityRecordBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicInteroperabilityRecordService.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final ProviderService providerService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    public PublicInteroperabilityRecordService(JmsService jmsService,
                                               PidIssuer pidIssuer,
                                               ProviderService providerService) {
        super(InteroperabilityRecordBundle.class);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.providerService = providerService;
    }

    @Override
    public String getResourceTypeName() {
        return "interoperability_record";
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        String lowerLevelResourceId = interoperabilityRecordBundle.getId();
        interoperabilityRecordBundle.setId(interoperabilityRecordBundle.getIdentifiers().getPid());
        interoperabilityRecordBundle.getMetadata().setPublished(true);

        // sets public id to providerId
        updateIdsToPublic(interoperabilityRecordBundle);

        // POST PID
        if (pidServiceEnabled) {
            logger.info("Posting InteroperabilityRecord with id {} to PID service", interoperabilityRecordBundle.getId());
            pidIssuer.postPID(interoperabilityRecordBundle.getId(), null);
        }

        InteroperabilityRecordBundle ret;
        logger.info("Interoperability Record '{}' is being published with id '{}'", lowerLevelResourceId, interoperabilityRecordBundle.getId());
        ret = super.add(interoperabilityRecordBundle, null);
        jmsService.convertAndSendTopic("interoperability_record.create", interoperabilityRecordBundle);
        return ret;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        InteroperabilityRecordBundle published = super.get(interoperabilityRecordBundle.getIdentifiers().getPid(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), true);
        InteroperabilityRecordBundle ret = super.get(interoperabilityRecordBundle.getIdentifiers().getPid(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, interoperabilityRecordBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public id to providerId
        updateIdsToPublic(interoperabilityRecordBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Interoperability Record with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try {
            InteroperabilityRecordBundle publicInteroperabilityRecordBundle = get(interoperabilityRecordBundle.getIdentifiers().getPid(),
                    interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), true);
            logger.info("Deleting public Interoperability Record with id '{}'", publicInteroperabilityRecordBundle.getId());
            super.delete(publicInteroperabilityRecordBundle);
            jmsService.convertAndSendTopic("interoperability_record.delete", publicInteroperabilityRecordBundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }


    @Override
    public void updateIdsToPublic(InteroperabilityRecordBundle bundle) {
        // providerId
        ProviderBundle providerBundle = providerService.get(bundle.getInteroperabilityRecord().getProviderId(),
                bundle.getInteroperabilityRecord().getCatalogueId(), false);
        bundle.getInteroperabilityRecord().setProviderId(providerBundle.getIdentifiers().getPid());
    }
}
