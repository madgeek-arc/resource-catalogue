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
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("publicAdapterManager")
public class PublicAdapterService extends AbstractPublicResourceManager<AdapterBundle> {

    private final ServiceService serviceService;

    public PublicAdapterService(GenericResourceService genericResourceService,
                                JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService,
                                ServiceService serviceService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.serviceService = serviceService;
    }

    @Override
    protected String getResourceTypeName() {
        return "adapter";
    }

    public void updateIdsToPublic(AdapterBundle adapter) {
        Object linkedResourceObj = adapter.getAdapter().get("linkedResource");
        if (!(linkedResourceObj instanceof Map<?, ?> linkedResource)) {
            return;
        }

        String linkedResourceType = (String) linkedResource.get("type");
        String linkedResourceId = (String) linkedResource.get("id");
        if (linkedResourceType == null || linkedResourceId == null) {
            return;
        }

        String publicId;
        if ("service".equalsIgnoreCase(linkedResourceType)) {
            ServiceBundle service = serviceService.get(linkedResourceId, adapter.getCatalogueId());
            publicId = service.getIdentifiers().getPid();
        } else if ("datasource".equalsIgnoreCase(linkedResourceType)) {
            //FIXME
//            InteroperabilityRecordBundle guideline = interoperabilityRecordService.get(linkedResourceId, adapter.getCatalogueId());
//            publicId = guideline.getIdentifiers().getPid();
        } else {
            return;
        }

//        linkedResource.put("id", publicId); //FIXME
    }

}