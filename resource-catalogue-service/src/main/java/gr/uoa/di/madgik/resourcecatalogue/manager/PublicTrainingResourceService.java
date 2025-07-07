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
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceService extends ResourceCatalogueManager<TrainingResourceBundle>
        implements PublicResourceService<TrainingResourceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicTrainingResourceService.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;
    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    public PublicTrainingResourceService(JmsService jmsService,
                                         PidIssuer pidIssuer,
                                         FacetLabelService facetLabelService,
                                         ProviderService providerService,
                                         @Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                                         @Lazy TrainingResourceService trainingResourceService) {
        super(TrainingResourceBundle.class);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.facetLabelService = facetLabelService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    @Override
    public Browsing<TrainingResourceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<TrainingResourceBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        String lowerLevelResourceId = trainingResourceBundle.getId();
        trainingResourceBundle.setId(trainingResourceBundle.getIdentifiers().getPid());
        trainingResourceBundle.getMetadata().setPublished(true);

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateIdsToPublic(trainingResourceBundle);

        if (pidServiceEnabled) {
            logger.info("Posting TrainingResource with id {} to PID service", trainingResourceBundle.getId());
            pidIssuer.postPID(trainingResourceBundle.getId(), null);
        }

        TrainingResourceBundle ret;
        logger.info("Training Resource '{}' is being published with id '{}'", lowerLevelResourceId, trainingResourceBundle.getId());
        ret = super.add(trainingResourceBundle, null);
        jmsService.convertAndSendTopic("training_resource.create", trainingResourceBundle);
        return ret;
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        TrainingResourceBundle published = super.get(trainingResourceBundle.getIdentifiers().getPid(),
                trainingResourceBundle.getTrainingResource().getCatalogueId(), true);
        TrainingResourceBundle ret = super.get(trainingResourceBundle.getIdentifiers().getPid(),
                trainingResourceBundle.getTrainingResource().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, trainingResourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateIdsToPublic(ret);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Training Resource with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("training_resource.update", ret);
        return ret;
    }

    @Override
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        try {
            TrainingResourceBundle publicTrainingResourceBundle = get(trainingResourceBundle.getIdentifiers().getPid(),
                    trainingResourceBundle.getTrainingResource().getCatalogueId(), true);
            logger.info("Deleting public Training Resource with id '{}'", publicTrainingResourceBundle.getId());
            super.delete(publicTrainingResourceBundle);
            jmsService.convertAndSendTopic("training_resource.delete", publicTrainingResourceBundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(TrainingResourceBundle bundle) {
        // Resource Organisation
        ProviderBundle providerBundle = providerService.get(bundle.getTrainingResource().getResourceOrganisation(),
                bundle.getTrainingResource().getCatalogueId(), false);
        bundle.getTrainingResource().setResourceOrganisation(providerBundle.getIdentifiers().getPid());

        // Resource Providers
        List<String> resourceProviders = new ArrayList<>();
        List<String> existingResourceProviders = bundle.getTrainingResource().getResourceProviders();
        if (existingResourceProviders != null && !existingResourceProviders.isEmpty()) {
            for (String resourceProviderId : existingResourceProviders) {
                //TODO: do we allow related resources from different catalogues?
                ProviderBundle resourceProvider = providerService.get(resourceProviderId,
                        bundle.getTrainingResource().getCatalogueId(), false);
                resourceProviders.add(resourceProvider.getIdentifiers().getPid());
            }
            bundle.getTrainingResource().setResourceProviders(resourceProviders);
        }

        // EOSC Related Services
        List<String> eoscRelatedServices = new ArrayList<>();
        List<String> existingEoscRelatedServices = bundle.getTrainingResource().getEoscRelatedServices();
        if (existingEoscRelatedServices != null && !existingEoscRelatedServices.isEmpty()) {
            for (String eoscRelatedServiceId : existingEoscRelatedServices) {
                //TODO: do we allow related resources from different catalogues?
                Bundle<?> eoscRelatedService;
                try {
                    eoscRelatedService = serviceBundleService.get(eoscRelatedServiceId,
                            bundle.getTrainingResource().getCatalogueId(), false);
                } catch (CatalogueResourceNotFoundException e) {
                    eoscRelatedService = trainingResourceService.get(eoscRelatedServiceId,
                            bundle.getTrainingResource().getCatalogueId(), false);
                }
                eoscRelatedServices.add(eoscRelatedService.getIdentifiers().getPid());
            }
            bundle.getTrainingResource().setEoscRelatedServices(eoscRelatedServices);
        }
    }
}
