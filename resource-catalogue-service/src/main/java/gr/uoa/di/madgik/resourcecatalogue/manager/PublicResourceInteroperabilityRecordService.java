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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service("publicResourceInteroperabilityRecordManager")
public class PublicResourceInteroperabilityRecordService
        extends AbstractPublicResourceManager<ResourceInteroperabilityRecordBundle> {

    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    public PublicResourceInteroperabilityRecordService(GenericResourceService genericResourceService,
                                                       JmsService jmsService,
                                                       PidIssuer pidIssuer,
                                                       FacetLabelService facetLabelService,
                                                       ServiceService serviceService,
                                                       DatasourceService datasourceService,
                                                       InteroperabilityRecordService interoperabilityRecordService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @Override
    public String getResourceTypeName() {
        return "resource_interoperability_record";
    }

    public void updateIdsToPublic(ResourceInteroperabilityRecordBundle bundle) {

        // resourceId
        Bundle resource;
        String resourceId = (String) bundle.getResourceInteroperabilityRecord().get("resourceId");
        try {
            resource = serviceService.get(resourceId, bundle.getCatalogueId());
        } catch (ResourceException e) {
            resource = datasourceService.get(resourceId, bundle.getCatalogueId());
        }
        bundle.getResourceInteroperabilityRecord().put("resourceId", resource.getIdentifiers().getPid());

        // Interoperability Record IDs
        List<String> interoperabilityRecordIds = new ArrayList<>();
        Object interoperabilityRecordIdsObj = bundle.getResourceInteroperabilityRecord().get("interoperabilityRecordIds");
        if (interoperabilityRecordIdsObj instanceof Collection<?>) {
            for (Object idObj : (Collection<?>) interoperabilityRecordIdsObj) {
                String interoperabilityRecordId = (String) idObj;
                InteroperabilityRecordBundle interoperabilityRecord = interoperabilityRecordService
                        .get(interoperabilityRecordId, bundle.getCatalogueId());
                interoperabilityRecordIds.add(interoperabilityRecord.getIdentifiers().getPid());
            }
        }
        bundle.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", interoperabilityRecordIds);
    }
}