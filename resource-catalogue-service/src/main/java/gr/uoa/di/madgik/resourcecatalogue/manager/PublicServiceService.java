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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
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

@Service("publicServiceManager")
public class PublicServiceService extends ResourceManager<ServiceBundle>
        implements PublicResourceService<ServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicServiceService.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;

    public PublicServiceService(JmsService jmsService,
                                ProviderResourcesCommonMethods commonMethods,
                                FacetLabelService facetLabelService) {
        super(ServiceBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceTypeName() {
        return "service";
    }

    @Override
    public Browsing<ServiceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<ServiceBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public ServiceBundle add(ServiceBundle serviceBundle, Authentication authentication) {
        String lowerLevelResourceId = serviceBundle.getId();
        Identifiers.createOriginalId(serviceBundle);
        serviceBundle.setId(PublicResourceUtils.createPublicResourceId(serviceBundle.getService().getId(),
                serviceBundle.getService().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(serviceBundle.getId(), serviceBundle.getService().getCatalogueId());

        // sets public ids to resource organisation, resource providers and related/required resources
        updateIdsToPublic(serviceBundle);

        serviceBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : serviceBundle.getService().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Service with id '{}' does not have a PID registered under its AlternativeIdentifiers.",
                    serviceBundle.getId());
        } else {
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        ServiceBundle ret;
        logger.info("Service '{}' is being published with id '{}'", lowerLevelResourceId, serviceBundle.getId());
        ret = super.add(serviceBundle, null);
        jmsService.convertAndSendTopic("service.create", serviceBundle);
        return ret;
    }

    @Override
    public ServiceBundle update(ServiceBundle serviceBundle, Authentication authentication) {
        ServiceBundle published = super.get(PublicResourceUtils.createPublicResourceId(serviceBundle.getService().getId(),
                serviceBundle.getService().getCatalogueId()));
        ServiceBundle ret = super.get(PublicResourceUtils.createPublicResourceId(serviceBundle.getService().getId(),
                serviceBundle.getService().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, serviceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to resource organisation, resource providers and related/required resources
        updateIdsToPublic(ret);

        ret.getService().setAlternativeIdentifiers(published.getService().getAlternativeIdentifiers());
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Service with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("service.update", ret);
        return ret;
    }

    @Override
    public void delete(ServiceBundle serviceBundle) {
        try {
            ServiceBundle publicServiceBundle = get(PublicResourceUtils.createPublicResourceId(
                    serviceBundle.getService().getId(),
                    serviceBundle.getService().getCatalogueId()));
            logger.info("Deleting public Service with id '{}'", publicServiceBundle.getId());
            super.delete(publicServiceBundle);
            jmsService.convertAndSendTopic("service.delete", publicServiceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(ServiceBundle bundle) {
        // Resource Organisation
        bundle.getService().setResourceOrganisation(PublicResourceUtils.createPublicResourceId(
                bundle.getService().getResourceOrganisation(), bundle.getService().getCatalogueId()));

        // Resource Providers
        bundle.getService().setResourceProviders(
                appendCatalogueId(
                        bundle.getService().getResourceProviders(),
                        bundle.getService().getCatalogueId()));

        // Related Resources
        bundle.getService().setRelatedResources(
                appendCatalogueId(
                        bundle.getService().getRelatedResources(),
                        bundle.getService().getCatalogueId()));

        // Required Resources
        bundle.getService().setRequiredResources(
                appendCatalogueId(
                        bundle.getService().getRequiredResources(),
                        bundle.getService().getCatalogueId()));
    }
}
