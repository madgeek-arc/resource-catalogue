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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("publicAdapterManager")
public class PublicAdapterService extends AbstractPublicResourceManager<AdapterBundle> {

    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final InteroperabilityRecordService guidelineService;

    public PublicAdapterService(GenericResourceService genericResourceService,
                                JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService,
                                ServiceService serviceService,
                                DatasourceService datasourceService,
                                InteroperabilityRecordService guidelineService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.guidelineService = guidelineService;
    }

    @Override
    protected String getResourceTypeName() {
        return "adapter";
    }

    @SuppressWarnings("unchecked")
    public void updateIdsToPublic(AdapterBundle adapter) {
        Map<String, Object> adapterMap = adapter.getAdapter();
        if (adapterMap == null) {
            return;
        }
        Object linkedResourceObj = adapterMap.get("linkedResource");
        if (!(linkedResourceObj instanceof Map)) {
            return;
        }

        Map<String, Object> linkedResource = (Map<String, Object>) linkedResourceObj;
        Object typeObj = linkedResource.get("type");
        Object idObj = linkedResource.get("id");
        if (!(typeObj instanceof String) || !(idObj instanceof String)) {
            return;
        }
        String type = (String) typeObj;
        String id = (String) idObj;

        String publicId;
        switch (type.toLowerCase()) {
            case "service" -> {
                ServiceBundle service = serviceService.get(id, adapter.getCatalogueId());
                publicId = service.getIdentifiers().getPid();
            }
            case "datasource" -> {
                DatasourceBundle datasource = datasourceService.get(id, adapter.getCatalogueId());
                publicId = datasource.getIdentifiers().getPid();
            }
            default -> {
                InteroperabilityRecordBundle guideline = guidelineService.get(id, adapter.getCatalogueId());
                publicId = guideline.getIdentifiers().getPid();
            }
        }
        linkedResource.put("id", publicId);
    }
}