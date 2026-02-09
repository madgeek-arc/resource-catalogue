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
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final RelationshipValidator relationshipValidator;
    private final GenericResourceService genericResourceService;
    private final WorkflowService workflowService;

    public TrainingResourceManager(ProviderService providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   VocabularyService vocabularyService,
                                   SynchronizerService<TrainingResource> synchronizerService,
                                   @Lazy ProviderResourcesCommonMethods commonMethods,
                                   @Lazy RelationshipValidator relationshipValidator,
                                   GenericResourceService genericResourceService,
                                   WorkflowService workflowService) {
        super(genericResourceService, securityService, vocabularyService);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.relationshipValidator = relationshipValidator;
        this.genericResourceService = genericResourceService;
        this.workflowService = workflowService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    //region generic
    @Override
    public TrainingResourceBundle add(TrainingResourceBundle bundle, Authentication auth) {
//        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("owner"),
//                trainingResource.getCatalogueId());
//        onboard(trainingResource, provider, auth);
//        onboardingValidation(trainingResource, provider);
//        TrainingResourceBundle ret = genericResourceService.add(getResourceTypeName(), trainingResource);
        TrainingResourceBundle ret = super.add(bundle, auth);
        onboardingValidation(bundle);
        try {
            ret = workflowService.onboard(getResourceTypeName(), ret, auth);
        } catch (ResourceException e) {
            genericResourceService.delete(getResourceTypeName(), bundle.getId());
            throw e;
        }
        this.update(ret, auth); // adds logging info - possibly replace with generic update
        return ret;
    }

    private void onboardingValidation(TrainingResourceBundle trainingResource) {
        relationshipValidator.checkRelatedResourceIDsConsistency(trainingResource);
        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();
    }

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
        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("owner"),
                trainingResource.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (trainingResource.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                trainingResource.setStatus(vocabularyService.get("pending").getId());
                trainingResource.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    @Transactional
    public void delete(TrainingResourceBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        commonMethods.deleteResourceInteroperabilityRecords(bundle.getId(), getResourceTypeName());
        logger.info("Deleting Training Resource: {} and all its Resource Interoperability Records", bundle.getId()); //TODO: can TRs be connected to IGs?
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
        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("owner"),
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
        providerService.update(provider, "system update", auth);
    }

    @Override
    public TrainingResourceBundle setActive(String id, Boolean active, Authentication auth) {
        TrainingResourceBundle existing = get(id);

        ProviderBundle provider = providerService.get((String) existing.getTrainingResource().get("owner"),
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
    public Paging<TrainingResourceBundle> getAllEOSCResourcesOfAProvider(String providerId, String catalogueId,
                                                                         int quantity, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.setQuantity(quantity);
        ff.addOrderBy("name", "asc");
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        TrainingResourceBundle trainingResource = get(id);
        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("owner"),
                trainingResource.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Training Resources", provider.getId());
//        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(trainingResource, provider); //FIXME
    }

    @Override
    public Browsing<TrainingResourceBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    //FIXME
    @Override
    public List<TrainingResourceBundle> getByIds(Authentication auth, String... ids) {
        List<TrainingResourceBundle> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId);
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
        ff.addFilter("owner", providerId);
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

    //region Drafts
    @Override
    public TrainingResourceBundle addDraft(TrainingResourceBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        TrainingResourceBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public TrainingResourceBundle updateDraft(TrainingResourceBundle bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            TrainingResourceBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(TrainingResourceBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public TrainingResourceBundle finalizeDraft(TrainingResourceBundle trainingResource, Authentication auth) {
        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("owner"),
                trainingResource.getCatalogueId());
        UserInfo user = UserInfo.of(auth);
        if (provider.getTemplateStatus().equals("approved template")) {
            trainingResource.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
        } else {
            trainingResource.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
        }
        trainingResource = update(trainingResource, auth);

        return trainingResource;
    }
    //endregion
}
