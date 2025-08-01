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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RelationshipValidator {

    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    @Autowired
    public RelationshipValidator(
            ProviderService providerService,
            ServiceBundleService<ServiceBundle> serviceBundleService,
            TrainingResourceService trainingResourceService,
            InteroperabilityRecordService interoperabilityRecordService
    ) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    //TODO: decide if we still want public IDs inside lower level resources
    public void checkRelatedResourceIDsConsistency(Object o) {
        String catalogueId = null;
        List<String> resourceProviders = new ArrayList<>();
        List<String> requiredResources = new ArrayList<>();
        List<String> relatedResources = new ArrayList<>();
        List<String> eoscRelatedServices = new ArrayList<>();
        List<String> interoperabilityRecordIds = new ArrayList<>();
        if (o != null) {
            if (o instanceof ServiceBundle) {
                catalogueId = ((ServiceBundle) o).getService().getCatalogueId();
                resourceProviders = ((ServiceBundle) o).getService().getResourceProviders();
                requiredResources = ((ServiceBundle) o).getService().getRequiredResources();
                relatedResources = ((ServiceBundle) o).getService().getRelatedResources();
            }
            if (o instanceof TrainingResourceBundle) {
                catalogueId = ((TrainingResourceBundle) o).getTrainingResource().getCatalogueId();
                resourceProviders = ((TrainingResourceBundle) o).getTrainingResource().getResourceProviders();
                eoscRelatedServices = ((TrainingResourceBundle) o).getTrainingResource().getEoscRelatedServices();
            }
            if (o instanceof ResourceInteroperabilityRecordBundle) {
                catalogueId = ((ResourceInteroperabilityRecordBundle) o).getResourceInteroperabilityRecord().getCatalogueId();
                interoperabilityRecordIds = ((ResourceInteroperabilityRecordBundle) o).getResourceInteroperabilityRecord().getInteroperabilityRecordIds();
            }
            if (resourceProviders != null && !resourceProviders.isEmpty() && resourceProviders.stream().anyMatch(Objects::nonNull)) {
                for (String resourceProvider : resourceProviders) {
                    if (resourceProvider != null && !resourceProvider.isEmpty()) {
                        try {
                            providerService.get(resourceProvider, catalogueId, false);
                        } catch (CatalogueResourceNotFoundException e) {
                            throw new ValidationException(String.format("Field [resourceProviders]: " +
                                            "There is no Provider with ID '%s' in the %s Catalogue.",
                                    resourceProvider, catalogueId));
                        }
                    }
                }
            }
            if (requiredResources != null && !requiredResources.isEmpty() && requiredResources.stream().anyMatch(Objects::nonNull)) {
                for (String requiredResource : requiredResources) {
                    if (requiredResource != null && !requiredResource.isEmpty()) {
                        try {
                            serviceBundleService.get(requiredResource, catalogueId, false);
                        } catch (CatalogueResourceNotFoundException e) {
                            try {
                                trainingResourceService.get(requiredResource, catalogueId, false);
                            } catch (CatalogueResourceNotFoundException j) {
                                throw new ValidationException(String.format("Field [requiredResources]: " +
                                                "There is no Service or Training Resource with ID '%s' in the %s Catalogue.",
                                        requiredResource, catalogueId));
                            }
                        }
                    }
                }
            }
            if (relatedResources != null && !relatedResources.isEmpty() && relatedResources.stream().anyMatch(Objects::nonNull)) {
                for (String relatedResource : relatedResources) {
                    if (relatedResource != null && !relatedResource.isEmpty()) {
                        try {
                            serviceBundleService.get(relatedResource, catalogueId, false);
                        } catch (CatalogueResourceNotFoundException e) {
                            try {
                                trainingResourceService.get(relatedResource, catalogueId, false);
                            } catch (CatalogueResourceNotFoundException j) {
                                throw new ValidationException(String.format("Field [relatedResources]: " +
                                                "There is no Service or Training Resource with ID '%s' in the %s Catalogue.",
                                        relatedResource, catalogueId));
                            }
                        }
                    }
                }
            }
            if (eoscRelatedServices != null && !eoscRelatedServices.isEmpty() && eoscRelatedServices.stream().anyMatch(Objects::nonNull)) {
                for (String eoscRelatedService : eoscRelatedServices) {
                    if (eoscRelatedService != null && !eoscRelatedService.isEmpty()) {
                        try {
                            serviceBundleService.get(eoscRelatedService, catalogueId, false);
                        } catch (CatalogueResourceNotFoundException e) {
                            try {
                                trainingResourceService.get(eoscRelatedService, catalogueId, false);
                            } catch (CatalogueResourceNotFoundException j) {
                                throw new ValidationException(String.format("Field [eoscRelatedServices]: " +
                                                "There is no Service or Training Resource with ID '%s' in the %s Catalogue. ",
                                        eoscRelatedService, catalogueId));
                            }
                        }
                    }
                }
            }
            if (interoperabilityRecordIds != null && !interoperabilityRecordIds.isEmpty() && interoperabilityRecordIds.stream().anyMatch(Objects::nonNull)) {
                for (String interoperabilityRecordId : interoperabilityRecordIds) {
                    if (interoperabilityRecordId != null && !interoperabilityRecordId.isEmpty()) {
                        try {
                            interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false);
                        } catch (CatalogueResourceNotFoundException e) {
                            throw new ValidationException(String.format("Field [interoperabilityRecordIds]: " +
                                            "There is no Interoperability Record with ID '%s' in the %s Catalogue.",
                                    interoperabilityRecordId, catalogueId));
                        }
                    }
                }
            }
        }
    }
}
