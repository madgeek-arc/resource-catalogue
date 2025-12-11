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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicServiceManager")
public class PublicServiceService
        extends AbstractPublicResourceManager<ServiceBundle>
        implements PublicResourceService<ServiceBundle> {

    private final ProviderService providerService;
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    public PublicServiceService(JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService,
                                ProviderService providerService,
                                ServiceBundleService serviceBundleService,
                                TrainingResourceService trainingResourceService) {
        super(ServiceBundle.class, jmsService, pidIssuer, facetLabelService);
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "service";
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