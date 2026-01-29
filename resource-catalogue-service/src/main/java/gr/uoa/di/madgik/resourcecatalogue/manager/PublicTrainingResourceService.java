/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceService extends AbstractPublicResourceManager<TrainingResourceBundle> {

    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;

    public PublicTrainingResourceService(GenericResourceService genericResourceService,
                                         JmsService jmsService,
                                         PidIssuer pidIssuer,
                                         FacetLabelService facetLabelService,
                                         ProviderService providerService,
                                         ServiceService serviceService,
                                         TrainingResourceService trainingResourceService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    @Override
    public void updateIdsToPublic(TrainingResourceBundle bundle) {
        // Owner
        ProviderBundle provider = providerService.get(
                (String) bundle.getTrainingResource().get("owner"),
                bundle.getCatalogueId()
        );
        bundle.getTrainingResource().put("owner", provider.getIdentifiers().getPid());

        // EOSC Related Services
        List<String> eoscRelatedServices = new ArrayList<>();
        Object existingObj = bundle.getTrainingResource().get("eoscRelatedServices");
        if (existingObj instanceof Collection<?>) {
            for (Object eoscRelatedServiceIdObj : (Collection<?>) existingObj) {
                String eoscRelatedServiceId = (String) eoscRelatedServiceIdObj;
                Bundle eoscRelatedService;
                try {
                    eoscRelatedService = serviceService.get(eoscRelatedServiceId, bundle.getCatalogueId());
                } catch (Exception e) { // TODO: specify the Exception
                    eoscRelatedService = trainingResourceService.get(eoscRelatedServiceId, bundle.getCatalogueId());
                }
                eoscRelatedServices.add(eoscRelatedService.getIdentifiers().getPid());
            }
            bundle.getTrainingResource().put("eoscRelatedServices", eoscRelatedServices);
        }
    }
}