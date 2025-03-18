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
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicProviderManager")
public class PublicProviderManager extends ResourceManager<ProviderBundle> implements ResourceCRUDService<ProviderBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicProviderManager.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;

    public PublicProviderManager(JmsService jmsService,
                                 ProviderResourcesCommonMethods commonMethods,
                                 FacetLabelService facetLabelService) {
        super(ProviderBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceTypeName() {
        return "provider";
    }

    @Override
    public Browsing<ProviderBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<ProviderBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public ProviderBundle add(ProviderBundle providerBundle, Authentication authentication) {
        String lowerLevelProviderId = providerBundle.getId();
        Identifiers.createOriginalId(providerBundle);
        providerBundle.setId(PublicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(providerBundle.getId(), providerBundle.getProvider().getCatalogueId());
        providerBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : providerBundle.getProvider().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Provider with id '{}' does not have a PID registered under its AlternativeIdentifiers.",
                    providerBundle.getId());
        } else {
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        ProviderBundle ret;
        logger.info("Provider '{}' is being published with id '{}'", lowerLevelProviderId, providerBundle.getId());
        ret = super.add(providerBundle, null);
        jmsService.convertAndSendTopic("provider.create", providerBundle);
        return ret;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication authentication) {
        ProviderBundle published = super.get(PublicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId()));
        ProviderBundle ret = super.get(PublicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, providerBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        ret.getProvider().setAlternativeIdentifiers(published.getProvider().getAlternativeIdentifiers());
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Provider with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("provider.asdf", ret);
        return ret;
    }

    @Override
    public void delete(ProviderBundle providerBundle) {
        try {
            ProviderBundle publicProviderBundle = get(PublicResourceUtils.createPublicResourceId(
                    providerBundle.getProvider().getId(),
                    providerBundle.getProvider().getCatalogueId()));
            logger.info("Deleting public Provider with id '{}'", publicProviderBundle.getId());
            super.delete(publicProviderBundle);
            jmsService.convertAndSendTopic("provider.delete", publicProviderBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}