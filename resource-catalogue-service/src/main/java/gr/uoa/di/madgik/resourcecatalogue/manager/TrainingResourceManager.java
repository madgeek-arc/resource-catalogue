/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service("trainingResourceManager")
public class TrainingResourceManager extends ResourceCatalogueGenericManager<TrainingResourceBundle> implements TrainingResourceService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingResourceManager.class);

    private final OrganisationService organisationService;
    private final RelationshipValidator relationshipValidator;
    private final GenericResourceService genericResourceService;
    private final EmailService emailService;

    public TrainingResourceManager(OrganisationService organisationService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   VocabularyService vocabularyService,
                                   @Lazy RelationshipValidator relationshipValidator,
                                   GenericResourceService genericResourceService,
                                   EmailService emailService,
                                   WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.organisationService = organisationService;
        this.relationshipValidator = relationshipValidator;
        this.genericResourceService = genericResourceService;
        this.emailService = emailService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    //region generic
    @Override
    @Transactional
    public TrainingResourceBundle update(TrainingResourceBundle trainingResource, String comment, Authentication auth) {
        TrainingResourceBundle existing = get(trainingResource.getId(), trainingResource.getCatalogueId());
        // check if there are actual changes in the Training Resource
        if (trainingResource.equals(existing)) {
            return trainingResource;
        }
        trainingResource.markUpdate(UserInfo.of(auth), comment);
        relationshipValidator.checkRelatedResourceIDsConsistency(trainingResource);
        checkAndResetServiceOnboarding(trainingResource, auth);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();

        try {
            return genericResourceService.update(getResourceTypeName(), trainingResource.getId(), trainingResource);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetServiceOnboarding(TrainingResourceBundle trainingResource, Authentication auth) {
        OrganisationBundle provider = organisationService.get((String) trainingResource.getTrainingResource().get("resourceOwner"),
                trainingResource.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (trainingResource.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                trainingResource.setStatus(vocabularyService.get("pending").getId());
                trainingResource.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                organisationService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    @Transactional
    public void delete(TrainingResourceBundle bundle) {
        blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Training Resource: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public TrainingResourceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        TrainingResourceBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Training Resource: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(TrainingResourceBundle trainingResource, String status, Authentication auth) {
        OrganisationBundle provider = organisationService.get((String) trainingResource.getTrainingResource().get("resourceOwner"),
                trainingResource.getCatalogueId());
        switch (status) {
            case "pending":
                provider.setTemplateStatus("pending template");
                break;
            case "approved":
                provider.setTemplateStatus("approved template");
                break;
            case "rejected":
                provider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        organisationService.update(provider, "system update", auth);
    }

    @Override
    public TrainingResourceBundle setActive(String id, Boolean active, Authentication auth) {
        TrainingResourceBundle existing = get(id);

        OrganisationBundle provider = organisationService.get((String) existing.getTrainingResource().get("resourceOwner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Training Resource, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Training Resource, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    //endregion

    //region EOSC Resource-specific
    @Override
    public Paging<TrainingResourceBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        TrainingResourceBundle trainingResource = get(id);
        OrganisationBundle provider = organisationService.get((String) trainingResource.getTrainingResource().get("resourceOwner"),
                trainingResource.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Training Resources", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(trainingResource, provider);
    }

    @Override
    public Browsing<TrainingResourceBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    @Override
    public List<TrainingResourceBundle> getByIds(Authentication auth, String... ids) {
        List<TrainingResourceBundle> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return resources;
    }

    @Override
    public Bundle getTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<TrainingResourceBundle> allProviderTrainingResources = getAll(ff, auth).getResults();
        for (TrainingResourceBundle bundle : allProviderTrainingResources) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }
    //endregion
}
