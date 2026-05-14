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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RelationshipValidator {

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    @Autowired
    public RelationshipValidator(OrganisationService organisationService,
                                 ServiceService serviceService,
                                 TrainingResourceService trainingResourceService,
                                 InteroperabilityRecordService interoperabilityRecordService) {
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    //TODO: decide if we still want public IDs inside lower level resources
    public void checkRelatedResourceIDsConsistency(Object o) {
        String catalogueId = null;
        List<String> serviceProviders = new ArrayList<>();
        List<String> eoscRelatedServices = new ArrayList<>();
        List<String> interoperabilityRecordIds = new ArrayList<>();
        if (o != null) {
            if (o instanceof ServiceBundle) {
                catalogueId = ((ServiceBundle) o).getCatalogueId();
                Object serviceProvidersObj = ((ServiceBundle) o).getService().get("serviceProviders");
                if (serviceProvidersObj instanceof List<?>) {
                    serviceProviders = (List<String>) serviceProvidersObj;
                }
            }
            if (o instanceof DatasourceBundle) {
                catalogueId = ((DatasourceBundle) o).getCatalogueId();
                Object serviceProvidersObj = ((DatasourceBundle) o).getDatasource().get("serviceProviders");
                if (serviceProvidersObj instanceof List<?>) {
                    serviceProviders = (List<String>) serviceProvidersObj;
                }
            }
            if (o instanceof CatalogueBundle) {
                catalogueId = ((CatalogueBundle) o).getCatalogueId();
                Object serviceProvidersObj = ((CatalogueBundle) o).getCatalogue().get("serviceProviders");
                if (serviceProvidersObj instanceof List<?>) {
                    serviceProviders = (List<String>) serviceProvidersObj;
                }
            }
            if (o instanceof TrainingResourceBundle) {
                catalogueId = ((TrainingResourceBundle) o).getCatalogueId();
                Object eoscRelatedServicesObj = ((TrainingResourceBundle) o).getTrainingResource().get("eoscRelatedServices");
                if (eoscRelatedServicesObj instanceof List<?>) {
                    eoscRelatedServices = (List<String>) eoscRelatedServicesObj;
                }
            }
            if (o instanceof ResourceInteroperabilityRecordBundle) {
                catalogueId = ((ResourceInteroperabilityRecordBundle) o).getCatalogueId();
                Object interoperabilityRecordIdsObj = ((ResourceInteroperabilityRecordBundle) o)
                        .getResourceInteroperabilityRecord().get("interoperabilityRecordIds");
                if (interoperabilityRecordIdsObj instanceof List<?>) {
                    interoperabilityRecordIds = (List<String>) interoperabilityRecordIdsObj;
                }
            }
            if (!serviceProviders.isEmpty() && serviceProviders.stream().anyMatch(Objects::nonNull)) {
                for (String serviceProvider : serviceProviders) {
                    if (serviceProvider != null && !serviceProvider.isEmpty()) {
                        try {
                            organisationService.get(serviceProvider, catalogueId);
                        } catch (CatalogueResourceNotFoundException e) {
                            throw new ValidationException(String.format("Field [resourceProviders]: " +
                                            "There is no Provider with ID '%s' in the %s Catalogue.",
                                    serviceProvider, catalogueId));
                        }
                    }
                }
            }
            if (!eoscRelatedServices.isEmpty() && eoscRelatedServices.stream().anyMatch(Objects::nonNull)) {
                for (String eoscRelatedService : eoscRelatedServices) {
                    if (eoscRelatedService != null && !eoscRelatedService.isEmpty()) {
                        try {
                            serviceService.get(eoscRelatedService, catalogueId);
                        } catch (CatalogueResourceNotFoundException e) {
                            throw new ValidationException(String.format("Field [eoscRelatedServices]: " +
                                            "There is no Service with ID '%s' in the %s Catalogue. ",
                                    eoscRelatedService, catalogueId));
                        }
                    }
                }
            }
            if (!interoperabilityRecordIds.isEmpty() && interoperabilityRecordIds.stream().anyMatch(Objects::nonNull)) {
                for (String interoperabilityRecordId : interoperabilityRecordIds) {
                    if (interoperabilityRecordId != null && !interoperabilityRecordId.isEmpty()) {
                        try {
                            interoperabilityRecordService.get(interoperabilityRecordId, catalogueId);
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
