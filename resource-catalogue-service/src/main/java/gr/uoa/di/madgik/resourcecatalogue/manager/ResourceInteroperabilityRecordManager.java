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

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import gr.uoa.di.madgik.resourcecatalogue.utils.ResourceValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceCatalogueGenericManager<ResourceInteroperabilityRecordBundle>
        implements ResourceInteroperabilityRecordService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final RelationshipValidator relationshipValidator;
    private final GenericResourceService genericResourceService;
    private final VocabularyService vocabularyService;
    private final ConfigurationTemplateService ctService;
    private final ConfigurationTemplateInstanceService ctiService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ResourceInteroperabilityRecordManager(ServiceService serviceService,
                                                 TrainingResourceService trainingResourceService,
                                                 InteroperabilityRecordService interoperabilityRecordService,
                                                 SecurityService securityService, ProviderResourcesCommonMethods commonMethods,
                                                 IdCreator idCreator, @Lazy RelationshipValidator relationshipValidator,
                                                 GenericResourceService genericResourceService,
                                                 VocabularyService vocabularyService,
                                                 ConfigurationTemplateService ctService,
                                                 ConfigurationTemplateInstanceService ctiService) {
        super(genericResourceService, idCreator, securityService, vocabularyService);
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.commonMethods = commonMethods;
        this.relationshipValidator = relationshipValidator;
        this.genericResourceService = genericResourceService;
        this.vocabularyService = vocabularyService;
        this.ctService = ctService;
        this.ctiService = ctiService;
    }

    public String getResourceTypeName() {
        return "resource_interoperability_record";
    }

    @Override
    public ResourceInteroperabilityRecordBundle getByResourceId(String resourceId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_id", resourceId);
        ff.addFilter("published", "false");
        try {
            ResourceInteroperabilityRecordBundle bundle = genericResourceService.get(getResourceTypeName(),
                    new SearchService.KeyValue("resource_id", resourceId),
                    new SearchService.KeyValue("published", "false"));
            return bundle;
        } catch (ResourceException e) {
            return null;
        }
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle bundle,
                                                    String resourceType,
                                                    Authentication auth) {
        validate(bundle, resourceType);
        relationshipValidator.checkRelatedResourceIDsConsistency(bundle);

        bundle.markOnboard(vocabularyService.get("approved").getId(), true, UserInfo.of(auth), null);
        bundle.setActive(true);
        bundle.setCatalogueId(this.catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        ResourceInteroperabilityRecordBundle ret = genericResourceService.add(getResourceTypeName(), bundle);
        return ret;
    }

    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle bundle, String comment, Authentication auth) {
        ResourceInteroperabilityRecordBundle existing = genericResourceService.get(getResourceTypeName(), bundle.getId());
        // check if there are actual changes in the Service
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(UserInfo.of(auth), comment);
        relationshipValidator.checkRelatedResourceIDsConsistency(bundle);

        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(ResourceInteroperabilityRecordBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Resource Interoperability Record: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());

    }

    @Override
    public ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle bundle, String resourceType) {
        String resourceId = (String) bundle.getResourceInteroperabilityRecord().get("resourceId");
        String catalogueId = (String) bundle.getResourceInteroperabilityRecord().get("catalogueId");

        ResourceInteroperabilityRecordBundle existing = getByResourceId(resourceId);
        if (existing != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Resource " +
                    "Interoperability Record registered, with id: [%s]", resourceId, catalogueId, existing.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceService, resourceType);
        } else if (resourceType.equals("training_resource")) { //TODO: probably remove
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else {
            throw new ValidationException("Field 'resourceType' should be either 'service' or 'training_resource'");
        }
        return checkIfEachInteroperabilityRecordIsApproved(bundle);
    }

    //TODO: test me
    @SuppressWarnings("unchecked")
    private ResourceInteroperabilityRecordBundle checkIfEachInteroperabilityRecordIsApproved(ResourceInteroperabilityRecordBundle bundle) {
        List<String> interoperabilityRecordIds = (List<String>) bundle.getResourceInteroperabilityRecord()
                .get("interoperabilityRecordIds");

        for (String id : interoperabilityRecordIds) {
            if (!"approved".equals(interoperabilityRecordService.get(id).getStatus())) {
                throw new ValidationException(
                        "One or more of the Interoperability Records you have provided is not yet approved."
                );
            }
        }
        return bundle;
    }

    @Override
    public void checkAndRemoveCTI(LinkedHashMap<String, Object> rir) {
        String resourceId = (String) rir.get("resourceId");

        @SuppressWarnings("unchecked")
        List<String> interoperabilityRecordIds =
                (List<String>) rir.get("interoperabilityRecordIds");
        Set<String> guidelineIds = new HashSet<>(interoperabilityRecordIds);
        deleteCTI(resourceId, guidelineIds);
    }

    @Override
    public void checkAndRemoveCTI(LinkedHashMap<String, Object> existingRIR, LinkedHashMap<String, Object> updatedRIR) {
        String resourceId = (String) existingRIR.get("resourceId");

        @SuppressWarnings("unchecked")
        List<String> exitingInteroperabilityRecordIds =
                (List<String>) existingRIR.get("interoperabilityRecordIds");
        Set<String> existingGuidelineList = new HashSet<>(exitingInteroperabilityRecordIds);

        @SuppressWarnings("unchecked")
        List<String> updatedInteroperabilityRecordIds =
                (List<String>) updatedRIR.get("interoperabilityRecordIds");
        Set<String> updatedGuidelineList = new HashSet<>(updatedInteroperabilityRecordIds);

        // If the lists are equal, do nothing
        if (new HashSet<>(existingGuidelineList).equals(new HashSet<>(updatedGuidelineList))) {
            return;
        }

        // Identify deleted Guideline IDs
        Set<String> missingGuidelineIds = new HashSet<>(existingGuidelineList);
        updatedGuidelineList.forEach(missingGuidelineIds::remove);
        if (missingGuidelineIds.isEmpty()) {
            return;
        }

        // Delete CTIs associated with the specific removed Guideline
        deleteCTI(resourceId, missingGuidelineIds);
    }

    private void deleteCTI(String resourceId, Set<String> guidelineIds) {
        for (String guidelineId : guidelineIds) {
            List<LinkedHashMap<String, Object>> ctList = ctService.getAllByInteroperabilityRecordId(null,
                    guidelineId).getResults();
            if (ctList == null || ctList.isEmpty()) {
                continue;
            }
            for (LinkedHashMap<String, Object> ct : ctList) {
                LinkedHashMap<String, Object> cti = ctiService.getByResourceAndConfigurationTemplateId(resourceId, (String) ct.get("id"));
                if (cti != null) {
                    try {
                        ConfigurationTemplateInstanceBundle ctiBundle = ctiService.get((String) cti.get("id"));
                        if (ctiBundle != null) {
                            logger.info("Deleting CTI with id '{}'", cti.get("id"));
                            ctiService.delete(ctiBundle);
                        }
                    } catch (Exception e) {
                        logger.info("Failed to delete CTI for ID {}: {}", cti.get("id"), e.getMessage());
                    }
                }
            }
        }
    }

    //region not-needed
    @Override
    public Browsing<ResourceInteroperabilityRecordBundle> getMy(FacetFilter filter, Authentication authentication) {
        return null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle verify(String id, String status, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle setActive(String id, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle addDraft(ResourceInteroperabilityRecordBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle updateDraft(ResourceInteroperabilityRecordBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public void deleteDraft(ResourceInteroperabilityRecordBundle bundle) {

    }

    @Override
    public ResourceInteroperabilityRecordBundle finalizeDraft(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth) {
        return null;
    }

    @Override
    public void addBulk(List<ResourceInteroperabilityRecordBundle> resources, Authentication auth) {
        super.addBulk(resources, auth);
    }

    @Override
    public void updateBulk(List<ResourceInteroperabilityRecordBundle> resources, Authentication auth) {
        super.updateBulk(resources, auth);
    }
    //endregion
}
