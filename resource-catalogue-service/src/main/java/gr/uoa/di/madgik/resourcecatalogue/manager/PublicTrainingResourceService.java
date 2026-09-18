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
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceService extends AbstractPublicResourceManager<TrainingResourceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicTrainingResourceService.class);

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final FederationLinkageService federationLinkageService;

    public PublicTrainingResourceService(GenericResourceService genericResourceService,
                                         JmsService jmsService,
                                         PidIssuer pidIssuer,
                                         FacetLabelService facetLabelService,
                                         OrganisationService organisationService,
                                         ServiceService serviceService,
                                         FederationLinkageService federationLinkageService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.federationLinkageService = federationLinkageService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    @Override
    public void updateIdsToPublic(TrainingResourceBundle bundle) {
        // Resource Owner
        bundle.getTrainingResource().put("resourceOwner", resolveOwnerPublicId(
                (String) bundle.getTrainingResource().get("resourceOwner"), bundle.getCatalogueId()));

        // EOSC Related Services
        List<String> eoscRelatedServices = new ArrayList<>();
        Object existingObj = bundle.getTrainingResource().get("eoscRelatedServices");
        if (existingObj instanceof Collection<?>) {
            for (Object eoscRelatedServiceIdObj : (Collection<?>) existingObj) {
                String eoscRelatedServiceId = (String) eoscRelatedServiceIdObj;
                eoscRelatedServices.add(toPublicId(eoscRelatedServiceId, bundle.getCatalogueId()));
            }
            bundle.getTrainingResource().put("eoscRelatedServices", eoscRelatedServices);
        }
    }

    /**
     * Resolves a locally-held Service id to its public PID. When the id doesn't resolve locally,
     * a PID-shaped id ({@code prefix/suffix}) is kept verbatim - it may already be a public PID
     * from another federation node. If the federation aggregator positively confirms the id
     * doesn't exist there either, it's still kept (rather than rejected), since that confirmation
     * may just mean the owning node is temporarily unreachable; the case is logged instead for
     * manual review. A non-PID-shaped unresolvable id is rethrown.
     */
    private String toPublicId(String serviceId, String catalogueId) {
        try {
            return serviceService.get(serviceId, catalogueId).getIdentifiers().getPid();
        } catch (ResourceException | ResourceNotFoundException e) {
            if (serviceId != null && serviceId.contains("/")) {
                Boolean exists = federationLinkageService.federatedResourceExists("service", serviceId);
                if (exists != null && !exists) {
                    logger.warn("Kept reference to id '{}' (type 'Service') which was not found locally and "
                            + "the federation aggregator confirmed it does not exist there either - may be a "
                            + "stale reference, or the owning federation node may be temporarily "
                            + "unreachable. Needs manual review.", serviceId);
                }
                return serviceId;
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