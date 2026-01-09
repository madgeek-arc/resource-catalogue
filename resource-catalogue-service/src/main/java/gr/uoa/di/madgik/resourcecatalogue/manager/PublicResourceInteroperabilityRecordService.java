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

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicResourceInteroperabilityRecordManager")
public class PublicResourceInteroperabilityRecordService
        extends AbstractPublicResourceManager<ResourceInteroperabilityRecordBundle>
        implements PublicResourceService<ResourceInteroperabilityRecordBundle> {

    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    public PublicResourceInteroperabilityRecordService(JmsService jmsService,
                                                       ServiceBundleService serviceBundleService,
                                                       TrainingResourceService trainingResourceService,
                                                       InteroperabilityRecordService interoperabilityRecordService,
                                                       PidIssuer pidIssuer,
                                                       FacetLabelService facetLabelService) {
        super(ResourceInteroperabilityRecordBundle.class, jmsService, pidIssuer, facetLabelService);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @Override
    public String getResourceTypeName() {
        return "resource_interoperability_record";
    }

    @Override
    public void updateIdsToPublic(ResourceInteroperabilityRecordBundle bundle) {
        // resourceId
        Bundle<?> resource;
        try {
            resource = serviceBundleService.get(bundle.getResourceInteroperabilityRecord().getResourceId(),
                    bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
        } catch (CatalogueResourceNotFoundException e) {
            resource = trainingResourceService.get(bundle.getResourceInteroperabilityRecord().getResourceId(),
                    bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
        }
        bundle.getResourceInteroperabilityRecord().setResourceId(resource.getIdentifiers().getPid());

        // Interoperability Record IDs
        List<String> interoperabilityRecordIds = new ArrayList<>();
        for (String interoperabilityRecordId : bundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()) {
            InteroperabilityRecordBundle interoperabilityRecord = interoperabilityRecordService.get(
                    interoperabilityRecordId, bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
            interoperabilityRecordIds.add(interoperabilityRecord.getIdentifiers().getPid());
        }
        bundle.getResourceInteroperabilityRecord().setInteroperabilityRecordIds(interoperabilityRecordIds);
    }
}
