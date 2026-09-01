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
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RelationshipValidator {

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final FederationLinkageService federationLinkageService;

    @Autowired
    public RelationshipValidator(OrganisationService organisationService,
                                 ServiceService serviceService,
                                 InteroperabilityRecordService interoperabilityRecordService,
                                 FederationLinkageService federationLinkageService) {
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.federationLinkageService = federationLinkageService;
    }

    /**
     * A cross-node reference stores the referenced resource's bare PID ({@code prefix/suffix}),
     * which does not resolve locally. Such an id is valid as long as the federation can see it -
     * or as long as the aggregator cannot be reached to say otherwise: a timeout / unreachable
     * aggregator returns {@code null} here, and we fail open rather than block a write on an
     * external dependency (matching {@code FederationResourceClient}'s posture). Only a definite
     * {@code FALSE} - the aggregator answered and the id is nowhere in the federation - is
     * treated as "does not exist".
     */
    private boolean existsInFederation(String resourceDisplayName, String id) {
        if (id == null || !id.contains("/")) {
            return false;
        }
        Boolean exists = federationLinkageService.federatedResourceExists(resourceDisplayName, id);
        return exists == null || exists;
    }

    //TODO: decide if we still want public IDs inside lower level resources

    /**
     * Validates every related-resource id carried by {@code o} (providers, {@code eoscRelatedServices},
     * {@code interoperabilityRecordIds}). Use on create.
     */
    public void checkRelatedResourceIDsConsistency(Object o) {
        checkRelatedResourceIDsConsistency(o, null);
    }

    /**
     * Update-aware variant: validates only the related-resource ids in {@code updated} that were
     * <em>not</em> already present on {@code existing}. An id that is unchanged since the last
     * write was validated then; re-checking it here would make an unrelated field edit depend on
     * (and potentially be blocked by) the federated-search aggregator. Pass {@code existing == null}
     * to validate everything (equivalent to {@link #checkRelatedResourceIDsConsistency(Object)}).
     */
    public void checkRelatedResourceIDsConsistency(Object updated, Object existing) {
        if (updated == null) {
            return;
        }
        RelatedIds now = extractRelatedIds(updated);
        RelatedIds alreadyValidated = extractRelatedIds(existing);

        validateProviders(newValues(now.serviceProviders(), alreadyValidated.serviceProviders()), now.catalogueId());
        validateServices(newValues(now.eoscRelatedServices(), alreadyValidated.eoscRelatedServices()), now.catalogueId());
        validateInteroperabilityRecords(
                newValues(now.interoperabilityRecordIds(), alreadyValidated.interoperabilityRecordIds()),
                now.catalogueId());
    }

    private void validateProviders(List<String> providerIds, String catalogueId) {
        for (String providerId : providerIds) {
            try {
                organisationService.get(providerId, catalogueId);
            } catch (ResourceNotFoundException e) {
                throw new ValidationException(String.format("Field [resourceProviders]: "
                        + "There is no Provider with ID '%s' in the %s Catalogue.", providerId, catalogueId));
            }
        }
    }

    private void validateServices(List<String> serviceIds, String catalogueId) {
        for (String serviceId : serviceIds) {
            try {
                serviceService.get(serviceId, catalogueId);
            } catch (ResourceNotFoundException e) {
                if (!existsInFederation("Service", serviceId)) {
                    throw new ValidationException(String.format("Field [eoscRelatedServices]: "
                            + "There is no Service with ID '%s' in the %s Catalogue or the federation. ",
                            serviceId, catalogueId));
                }
            }
        }
    }

    private void validateInteroperabilityRecords(List<String> interoperabilityRecordIds, String catalogueId) {
        for (String interoperabilityRecordId : interoperabilityRecordIds) {
            try {
                interoperabilityRecordService.get(interoperabilityRecordId, catalogueId);
            } catch (ResourceNotFoundException e) {
                if (!existsInFederation("Interoperability Record", interoperabilityRecordId)) {
                    throw new ValidationException(String.format("Field [interoperabilityRecordIds]: "
                            + "There is no Interoperability Record with ID '%s' in the %s Catalogue "
                            + "or the federation.", interoperabilityRecordId, catalogueId));
                }
            }
        }
    }

    /**
     * Non-blank ids in {@code current} that are not already in {@code alreadyValidated}.
     */
    private static List<String> newValues(List<String> current, List<String> alreadyValidated) {
        Set<String> known = new HashSet<>(alreadyValidated);
        List<String> out = new ArrayList<>();
        for (String value : current) {
            if (value != null && !value.isEmpty() && !known.contains(value)) {
                out.add(value);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private RelatedIds extractRelatedIds(Object o) {
        if (o == null) {
            return RelatedIds.EMPTY;
        }
        String catalogueId = null;
        List<String> serviceProviders = List.of();
        List<String> eoscRelatedServices = List.of();
        List<String> interoperabilityRecordIds = List.of();

        if (o instanceof ServiceBundle sb) {
            catalogueId = sb.getCatalogueId();
            serviceProviders = asStringList(sb.getService().get("serviceProviders"));
        } else if (o instanceof DatasourceBundle db) {
            catalogueId = db.getCatalogueId();
            serviceProviders = asStringList(db.getDatasource().get("serviceProviders"));
        } else if (o instanceof CatalogueBundle cb) {
            catalogueId = cb.getCatalogueId();
            serviceProviders = asStringList(cb.getCatalogue().get("serviceProviders"));
        } else if (o instanceof TrainingResourceBundle tb) {
            catalogueId = tb.getCatalogueId();
            eoscRelatedServices = asStringList(tb.getTrainingResource().get("eoscRelatedServices"));
        } else if (o instanceof ResourceInteroperabilityRecordBundle rb) {
            catalogueId = rb.getCatalogueId();
            interoperabilityRecordIds = asStringList(
                    rb.getResourceInteroperabilityRecord().get("interoperabilityRecordIds"));
        }
        return new RelatedIds(catalogueId, serviceProviders, eoscRelatedServices, interoperabilityRecordIds);
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object o) {
        return o instanceof List<?> list ? (List<String>) list : List.of();
    }

    private record RelatedIds(String catalogueId, List<String> serviceProviders,
                              List<String> eoscRelatedServices, List<String> interoperabilityRecordIds) {
        static final RelatedIds EMPTY = new RelatedIds(null, List.of(), List.of(), List.of());
    }
}
