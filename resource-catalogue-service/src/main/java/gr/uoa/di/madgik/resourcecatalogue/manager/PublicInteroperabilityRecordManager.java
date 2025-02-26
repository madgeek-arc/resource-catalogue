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
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicInteroperabilityRecordManager")
public class PublicInteroperabilityRecordManager extends AbstractPublicResourceManager<InteroperabilityRecordBundle>
        implements ResourceCRUDService<InteroperabilityRecordBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicInteroperabilityRecordManager.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicResourceUtils publicResourceUtils;

    public PublicInteroperabilityRecordManager(JmsService jmsService,
                                               ProviderResourcesCommonMethods commonMethods,
                                               PublicResourceUtils publicResourceUtils) {
        super(InteroperabilityRecordBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.publicResourceUtils = publicResourceUtils;
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
        Identifiers.createOriginalId(interoperabilityRecordBundle);
        interoperabilityRecordBundle.setId(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(interoperabilityRecordBundle.getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());

        // sets public id to providerId
        updateInteroperabilityRecordIdsToPublic(interoperabilityRecordBundle);

        interoperabilityRecordBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : interoperabilityRecordBundle.getInteroperabilityRecord().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Interoperability Record with id '{}' does not have a PID registered under its AlternativeIdentifiers.",
                    interoperabilityRecordBundle.getId());
        } else {
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        InteroperabilityRecordBundle ret;
        logger.info("Interoperability Record '{}' is being published with id '{}'", lowerLevelResourceId, interoperabilityRecordBundle.getId());
        ret = super.add(interoperabilityRecordBundle, null);
        jmsService.convertAndSendTopic("interoperability_record.create", interoperabilityRecordBundle);
        return ret;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        InteroperabilityRecordBundle published = super.get(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        InteroperabilityRecordBundle ret = super.get(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, interoperabilityRecordBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public id to providerId
        updateInteroperabilityRecordIdsToPublic(interoperabilityRecordBundle);

        ret.getInteroperabilityRecord().setAlternativeIdentifiers(published.getInteroperabilityRecord().getAlternativeIdentifiers());
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
            InteroperabilityRecordBundle publicInteroperabilityRecordBundle = get(publicResourceUtils.createPublicResourceId(
                    interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                    interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
            logger.info("Deleting public Interoperability Record with id '{}'", publicInteroperabilityRecordBundle.getId());
            super.delete(publicInteroperabilityRecordBundle);
            jmsService.convertAndSendTopic("interoperability_record.delete", publicInteroperabilityRecordBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

}
