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

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceService
        extends AbstractPublicResourceManager<TrainingResourceBundle>
        implements PublicResourceService<TrainingResourceBundle> {

    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    public PublicTrainingResourceService(JmsService jmsService,
                                         PidIssuer pidIssuer,
                                         FacetLabelService facetLabelService,
                                         ProviderService providerService,
                                         @Lazy ServiceService serviceService,
                                         @Lazy TrainingResourceService trainingResourceService) {
        super(TrainingResourceBundle.class, jmsService, pidIssuer, facetLabelService);
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    //FIXME
//    @Override
//    public void updateIdsToPublic(TrainingResourceBundle bundle) {
//        // Resource Organisation
//        NewProviderBundle providerBundle = providerService.get(bundle.getTrainingResource().getResourceOrganisation(),
//                bundle.getTrainingResource().getCatalogueId());
//        bundle.getTrainingResource().setResourceOrganisation(providerBundle.getIdentifiers().getPid());
//
//        // Resource Providers
//        List<String> resourceProviders = new ArrayList<>();
//        List<String> existingResourceProviders = bundle.getTrainingResource().getResourceProviders();
//        if (existingResourceProviders != null && !existingResourceProviders.isEmpty()) {
//            for (String resourceProviderId : existingResourceProviders) {
//                //TODO: do we allow related resources from different catalogues?
//                NewProviderBundle resourceProvider = providerService.get(resourceProviderId,
//                        bundle.getTrainingResource().getCatalogueId());
//                resourceProviders.add(resourceProvider.getIdentifiers().getPid());
//            }
//            bundle.getTrainingResource().setResourceProviders(resourceProviders);
//        }

        //FIXME
        // EOSC Related Services
//        List<String> eoscRelatedServices = new ArrayList<>();
//        List<String> existingEoscRelatedServices = bundle.getTrainingResource().getEoscRelatedServices();
//        if (existingEoscRelatedServices != null && !existingEoscRelatedServices.isEmpty()) {
//            for (String eoscRelatedServiceId : existingEoscRelatedServices) {
//                //TODO: do we allow related resources from different catalogues?
//                NewBundle eoscRelatedService;
//                try {
//                    eoscRelatedService = serviceService.get(eoscRelatedServiceId,
//                            bundle.getTrainingResource().getCatalogueId());
//                } catch (CatalogueResourceNotFoundException e) {
//                    eoscRelatedService = trainingResourceService.get(eoscRelatedServiceId,
//                            bundle.getTrainingResource().getCatalogueId(), false);
//                }
//                eoscRelatedServices.add(eoscRelatedService.getIdentifiers().getPid());
//            }
//            bundle.getTrainingResource().setEoscRelatedServices(eoscRelatedServices);
//        }
//    }
}