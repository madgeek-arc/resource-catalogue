/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.*;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceCatalogueManager<ResourceInteroperabilityRecordBundle>
        implements ResourceInteroperabilityRecordService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;
    private final RelationshipValidator relationshipValidator;
    private final ConfigurationTemplateService ctService;
    private final ConfigurationTemplateInstanceService ctiService;

    public ResourceInteroperabilityRecordManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                                                 TrainingResourceService trainingResourceService,
                                                 InteroperabilityRecordService interoperabilityRecordService,
                                                 SecurityService securityService, ProviderResourcesCommonMethods commonMethods,
                                                 PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager,
                                                 IdCreator idCreator, @Lazy RelationshipValidator relationshipValidator,
                                                 ConfigurationTemplateService ctService,
                                                 ConfigurationTemplateInstanceService ctiService) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.idCreator = idCreator;
        this.relationshipValidator = relationshipValidator;
        this.ctService = ctService;
        this.ctiService = ctiService;
    }

    @Override
    public String getResourceTypeName() {
        return "resource_interoperability_record";
    }

    @Override
    public ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle bundle, String resourceType) {
        String resourceId = bundle.getResourceInteroperabilityRecord().getResourceId();
        String catalogueId = bundle.getResourceInteroperabilityRecord().getCatalogueId();

        ResourceInteroperabilityRecordBundle existing = getWithResourceId(resourceId);
        if (existing != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Resource " +
                    "Interoperability Record registered, with id: [%s]", resourceId, catalogueId, existing.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else {
            throw new ValidationException("Field 'resourceType' should be either 'service' or 'training_resource'");
        }

        super.validate(bundle);
        return checkIfEachInteroperabilityRecordIsApproved(bundle);
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle bundle, String resourceType, Authentication auth) {
        validate(bundle, resourceType);
        relationshipValidator.checkRelatedResourceIDsConsistency(bundle);

        bundle.setId(idCreator.generate(getResourceTypeName()));
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);
        logger.trace("Attempting to add a new ResourceInteroperabilityRecord: {}", bundle);

        bundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.getFirst());

        // active
        bundle.setActive(true);

        ResourceInteroperabilityRecordBundle ret;
        ret = super.add(bundle, null);
        logger.debug("Adding ResourceInteroperabilityRecord: {}", bundle);

        return ret;
    }

    public ResourceInteroperabilityRecordBundle getWithResourceId(String resourceId) {
        Resource res = where(false,
                new SearchService.KeyValue("resource_id", resourceId),
//                new SearchService.KeyValue("catalogue_id", resourceId), //TODO: enable when RIR from different catalogues is supported
                new SearchService.KeyValue("published", "false"));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle bundle, Authentication auth) {
        logger.trace("Attempting to update the ResourceInteroperabilityRecord with id '{}'", bundle.getId());

        ResourceInteroperabilityRecordBundle ret = ObjectUtils.clone(bundle);
        Resource existingResource = getResource(bundle.getId(),
                bundle.getResourceInteroperabilityRecord().getCatalogueId(), false);
        ResourceInteroperabilityRecordBundle existingInteroperabilityRecord = deserialize(existingResource);
        // check if there are actual changes in the ResourceInteroperabilityRecord
        if (ret.getResourceInteroperabilityRecord().equals(existingInteroperabilityRecord.getResourceInteroperabilityRecord())) {
            return ret;
        }
        relationshipValidator.checkRelatedResourceIDsConsistency(ret);

        // block Public ResourceInteroperabilityRecordBundle updates
        if (ret.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Resource Interoperability Record");
        }

        validate(ret);
        checkIfEachInteroperabilityRecordIsApproved(ret);

        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        ret.setIdentifiers(existingInteroperabilityRecord.getIdentifiers());
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingInteroperabilityRecord, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(getResourceType());

        // block user from updating resourceId
        if (!ret.getResourceInteroperabilityRecord().getResourceId().equals(existingInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Resource Id with which this ResourceInteroperabilityRecord is related");
        }

        resourceService.updateResource(existingResource);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", ret);

        return ret;
    }

    @Override
    public void delete(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // block Public ResourceInteroperabilityRecordBundle deletions
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Resource Interoperability Record");
        }
        logger.trace("User is attempting to delete the ResourceInteroperabilityRecord with id '{}'",
                resourceInteroperabilityRecordBundle.getId());
        super.delete(resourceInteroperabilityRecordBundle);
        logger.debug("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

    }

    private ResourceInteroperabilityRecordBundle checkIfEachInteroperabilityRecordIsApproved(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        for (String interoperabilityRecord : resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()) {
            if (!interoperabilityRecordService.get(interoperabilityRecord).getStatus().equals("approved interoperability record")) {
                throw new ValidationException("One ore more of the Interoperability Records you have provided is not yet approved.");
            }
        }
        return resourceInteroperabilityRecordBundle;
    }

    @Override
    public void checkAndRemoveCTI(ResourceInteroperabilityRecord existingRIR, ResourceInteroperabilityRecord updatedRIR) {
        List<String> existingGuidelineList = existingRIR.getInteroperabilityRecordIds();
        List<String> updateGuidelineList = updatedRIR.getInteroperabilityRecordIds();

        // If the lists are equal, do nothing
        if (new HashSet<>(existingGuidelineList).equals(new HashSet<>(updateGuidelineList))) {
            return;
        }

        // Identify deleted Guideline IDs
        Set<String> missingGuidelineIds = new HashSet<>(existingGuidelineList);
        missingGuidelineIds.removeAll(updateGuidelineList);
        if (missingGuidelineIds.isEmpty()) {
            return;
        }

        // Delete CTI associated with the specific deleted Guideline
        for (String missingGuidelineId : missingGuidelineIds) {
            List<ConfigurationTemplate> ctList = ctService.getAllByInteroperabilityRecordId(null,
                    missingGuidelineId).getResults();
            if (ctList == null || ctList.isEmpty()) {
                continue;
            }
            for (ConfigurationTemplate ct : ctList) {
                List<ConfigurationTemplateInstance> ctiList = ctiService.getByConfigurationTemplateId(ct.getId());
                if (ctiList == null || ctiList.isEmpty()) {
                    continue;
                }
                for (ConfigurationTemplateInstance cti : ctiList) {
                    try {
                        ConfigurationTemplateInstanceBundle ctiBundle = ctiService.get(cti.getId());
                        if (ctiBundle != null) {
                            logger.info("Deleting CTI with id '{}'", cti.getId());
                            ctiService.delete(ctiBundle);
                        }
                    } catch (Exception e) {
                        logger.info("Failed to delete CTI for ID {}: {}", cti.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public ResourceInteroperabilityRecordBundle createPublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth) {
        publicResourceInteroperabilityRecordManager.add(resourceInteroperabilityRecordBundle, auth);
        return resourceInteroperabilityRecordBundle;
    }
}
