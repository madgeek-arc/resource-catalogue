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
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicServiceManager")
public class PublicServiceService extends ResourceCatalogueManager<ServiceBundle>
        implements PublicResourceService<ServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicServiceService.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;
    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    public PublicServiceService(JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService,
                                ProviderService providerService,
                                ServiceBundleService<ServiceBundle> serviceBundleService,
                                TrainingResourceService trainingResourceService) {
        super(ServiceBundle.class);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.facetLabelService = facetLabelService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
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
        serviceBundle.setId(serviceBundle.getIdentifiers().getPid());
        serviceBundle.getMetadata().setPublished(true);

        // sets public ids to resource organisation, resource providers and related/required resources
        updateIdsToPublic(serviceBundle);

        // POST PID
        logger.info("PID POST disabled");
//        pidIssuer.postPID(serviceBundle.getId(), null);

        ServiceBundle ret;
        logger.info("Service '{}' is being published with id '{}'", lowerLevelResourceId, serviceBundle.getId());
        ret = super.add(serviceBundle, null);
        jmsService.convertAndSendTopic("service.create", serviceBundle);
        return ret;
    }

    @Override
    public ServiceBundle update(ServiceBundle serviceBundle, Authentication authentication) {
        ServiceBundle published = super.get(serviceBundle.getIdentifiers().getPid(), serviceBundle.getService().getCatalogueId(), true);
        ServiceBundle ret = super.get(serviceBundle.getIdentifiers().getPid(), serviceBundle.getService().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, serviceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to resource organisation, resource providers and related/required resources
        updateIdsToPublic(ret);

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
            ServiceBundle publicServiceBundle = get(serviceBundle.getIdentifiers().getPid(),
                    serviceBundle.getService().getCatalogueId(), true);
            logger.info("Deleting public Service with id '{}'", publicServiceBundle.getId());
            super.delete(publicServiceBundle);
            jmsService.convertAndSendTopic("service.delete", publicServiceBundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(ServiceBundle bundle) {
        // Resource Organisation
        ProviderBundle providerBundle = providerService.get(bundle.getService().getResourceOrganisation(),
                bundle.getService().getCatalogueId(), false);
        bundle.getService().setResourceOrganisation(providerBundle.getIdentifiers().getPid());

        // Resource Providers
        List<String> resourceProviders = new ArrayList<>();
        List<String> existingResourceProviders = bundle.getService().getResourceProviders();
        if (existingResourceProviders != null && !existingResourceProviders.isEmpty()) {
            for (String resourceProviderId : existingResourceProviders) {
                //TODO: do we allow related resources from different catalogues?
                ProviderBundle resourceProvider = providerService.get(resourceProviderId, bundle.getService().getCatalogueId(), false);
                resourceProviders.add(resourceProvider.getIdentifiers().getPid());
            }
            bundle.getService().setResourceProviders(resourceProviders);
        }

        // Related Resources
        List<String> relatedResources = new ArrayList<>();
        List<String> existingRelatedResources = bundle.getService().getRelatedResources();
        if (existingRelatedResources != null && !existingRelatedResources.isEmpty()) {
            for (String relatedResourceId : existingRelatedResources) {
                //TODO: do we allow related resources from different catalogues?
                Bundle<?> relatedResource;
                try {
                    relatedResource = serviceBundleService.get(relatedResourceId, bundle.getService().getCatalogueId(), false);
                } catch (CatalogueResourceNotFoundException e) {
                    relatedResource = trainingResourceService.get(relatedResourceId, bundle.getService().getCatalogueId(), false);
                }
                relatedResources.add(relatedResource.getIdentifiers().getPid());
            }
            bundle.getService().setRelatedResources(relatedResources);
        }

        // Required Resources
        List<String> requiredResources = new ArrayList<>();
        List<String> existingRequiredResources = bundle.getService().getRequiredResources();
        if (existingRequiredResources != null && !existingRequiredResources.isEmpty()) {
            for (String requiredResourceId : existingRequiredResources) {
                //TODO: do we allow related resources from different catalogues?
                Bundle<?> requiredResource;
                try {
                    requiredResource = serviceBundleService.get(requiredResourceId, bundle.getService().getCatalogueId(), false);
                } catch (CatalogueResourceNotFoundException e) {
                    requiredResource = trainingResourceService.get(requiredResourceId, bundle.getService().getCatalogueId(), false);
                }
                requiredResources.add(requiredResource.getIdentifiers().getPid());
            }
            bundle.getService().setRequiredResources(requiredResources);
        }
    }
}
