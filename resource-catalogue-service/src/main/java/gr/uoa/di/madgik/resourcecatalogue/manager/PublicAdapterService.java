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
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("publicAdapterManager")
public class PublicAdapterService extends AbstractPublicResourceManager<AdapterBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicAdapterService.class);

    private final ServiceService serviceService;
    private final InteroperabilityRecordService guidelineService;
    private final OrganisationService organisationService;
    private final FederationLinkageService federationLinkageService;

    public PublicAdapterService(GenericResourceService genericResourceService,
                                JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService,
                                ServiceService serviceService,
                                InteroperabilityRecordService guidelineService,
                                OrganisationService organisationService,
                                FederationLinkageService federationLinkageService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.serviceService = serviceService;
        this.guidelineService = guidelineService;
        this.organisationService = organisationService;
        this.federationLinkageService = federationLinkageService;
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

        // Resource Owner
        adapterMap.put("resourceOwner", resolveOwnerPublicId(
                (String) adapterMap.get("resourceOwner"), adapter.getCatalogueId()));

        // Linked Resource
        Object linkedResourceObj = adapterMap.get("linkedResource");
        if (!(linkedResourceObj instanceof Map)) {
            return;
        }

        Map<String, Object> linkedResource = (Map<String, Object>) linkedResourceObj;
        Object typeObj = linkedResource.get("resource_type");
        Object idObj = linkedResource.get("id");
        if (!(typeObj instanceof String) || !(idObj instanceof String)) {
            return;
        }
        String resourceType = (String) typeObj;
        String id = (String) idObj;

        linkedResource.put("id", toPublicId(resourceType, id, adapter.getCatalogueId()));
    }

    /**
     * Resolves a locally-held linked resource id to its public PID. When the id doesn't resolve
     * locally, a PID-shaped id ({@code prefix/suffix}) is kept verbatim - it may already be a
     * public PID from another federation node. If the federation aggregator positively confirms
     * the id doesn't exist there either, it's still kept (rather than rejected), since that
     * confirmation may just mean the owning node is temporarily unreachable; the case is logged
     * instead for manual review. A non-PID-shaped unresolvable id is rethrown.
     */
    private String toPublicId(String resourceTypeName, String id, String catalogueId) {
        try {
            return switch (resourceTypeName.toLowerCase()) {
                case "service" -> {
                    ServiceBundle service = serviceService.get(id, catalogueId);
                    yield service.getIdentifiers().getPid();
                }
                case "interoperability_record" -> {
                    InteroperabilityRecordBundle guideline = guidelineService.get(id, catalogueId);
                    yield guideline.getIdentifiers().getPid();
                }
                default -> throw new IllegalArgumentException("Unsupported Resource Type: " + resourceTypeName);
            };
        } catch (ResourceException | ResourceNotFoundException e) {
            if (id != null && id.contains("/")) {
                Boolean exists = federationLinkageService.federatedResourceExists(resourceTypeName, id);
                if (exists != null && !exists) {
                    logger.warn("Kept reference to id '{}' (type '{}') which was not found locally and the "
                            + "federation aggregator confirmed it does not exist there either - may be a "
                            + "stale reference, or the owning federation node may be temporarily "
                            + "unreachable. Needs manual review.", id, resourceTypeName);
                }
                return id;
            }
            throw e;
        }
    }

    /**
     * Resolves a locally-held Organisation id to its public PID, with the same federation
     * fallback as {@link #toPublicId}.
     */
    private String resolveOwnerPublicId(String resourceOwnerId, String catalogueId) {
        try {
            return organisationService.get(resourceOwnerId, catalogueId).getIdentifiers().getPid();
        } catch (ResourceException | ResourceNotFoundException e) {
            if (resourceOwnerId != null && resourceOwnerId.contains("/")) {
                Boolean exists = federationLinkageService.federatedResourceExists("organisation", resourceOwnerId);
                if (exists != null && !exists) {
                    logger.warn("Kept reference to id '{}' (type 'organisation') which was not found locally "
                            + "and the federation aggregator confirmed it does not exist there either - may "
                            + "be a stale reference, or the owning federation node may be temporarily "
                            + "unreachable. Needs manual review.", resourceOwnerId);
                }
                return resourceOwnerId;
            }
            throw e;
        }
    }
}