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

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service("publicResourceInteroperabilityRecordManager")
public class PublicResourceInteroperabilityRecordService
        extends AbstractPublicResourceManager<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourceInteroperabilityRecordService.class);

    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final FederationLinkageService federationLinkageService;

    public PublicResourceInteroperabilityRecordService(GenericResourceService genericResourceService,
                                                       JmsService jmsService,
                                                       PidIssuer pidIssuer,
                                                       FacetLabelService facetLabelService,
                                                       ServiceService serviceService,
                                                       DatasourceService datasourceService,
                                                       InteroperabilityRecordService interoperabilityRecordService,
                                                       FederationLinkageService federationLinkageService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.federationLinkageService = federationLinkageService;
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
        } catch (ResourceException | ResourceNotFoundException e) {
            resource = datasourceService.get(resourceId, bundle.getCatalogueId());
        }
        bundle.getResourceInteroperabilityRecord().put("resourceId", resource.getIdentifiers().getPid());

        // Interoperability Record IDs
        List<String> interoperabilityRecordIds = new ArrayList<>();
        Object interoperabilityRecordIdsObj = bundle.getResourceInteroperabilityRecord().get("interoperabilityRecordIds");
        if (interoperabilityRecordIdsObj instanceof Collection<?>) {
            for (Object idObj : (Collection<?>) interoperabilityRecordIdsObj) {
                String interoperabilityRecordId = (String) idObj;
                interoperabilityRecordIds.add(toPublicId(interoperabilityRecordId, bundle.getCatalogueId()));
            }
        }
        bundle.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", interoperabilityRecordIds);
    }

    /**
     * Resolves a locally-held Interoperability Record id to its public PID. When the id doesn't
     * resolve locally, a PID-shaped id ({@code prefix/suffix}) is kept verbatim - it may already
     * be a public PID from another federation node. If the federation aggregator positively
     * confirms the id doesn't exist there either, it's still kept (rather than rejected), since
     * that confirmation may just mean the owning node is temporarily unreachable; the case is
     * logged instead for manual review. A non-PID-shaped unresolvable id is rethrown.
     */
    private String toPublicId(String interoperabilityRecordId, String catalogueId) {
        try {
            return interoperabilityRecordService.get(interoperabilityRecordId, catalogueId)
                    .getIdentifiers().getPid();
        } catch (ResourceException | ResourceNotFoundException e) {
            if (interoperabilityRecordId != null && interoperabilityRecordId.contains("/")) {
                Boolean exists = federationLinkageService.federatedResourceExists(
                        "Interoperability Record", interoperabilityRecordId);
                if (exists != null && !exists) {
                    logger.warn("Kept reference to id '{}' (type 'Interoperability Record') which was not "
                            + "found locally and the federation aggregator confirmed it does not exist there "
                            + "either - may be a stale reference, or the owning federation node may be "
                            + "temporarily unreachable. Needs manual review.", interoperabilityRecordId);
                }
                return interoperabilityRecordId;
            }
            throw e;
        }
    }
}