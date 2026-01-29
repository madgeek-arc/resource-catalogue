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
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final RelationshipValidator relationshipValidator;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public TrainingResourceManager(ProviderService providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy VocabularyService vocabularyService,
                                   SynchronizerService<TrainingResource> synchronizerService,
                                   @Lazy ProviderResourcesCommonMethods commonMethods,
                                   @Lazy RelationshipValidator relationshipValidator,
                                   GenericResourceService genericResourceService) {
        super(genericResourceService, securityService);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.relationshipValidator = relationshipValidator;
        this.genericResourceService = genericResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    //region generic
    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResource, Authentication auth) {
        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("owner"),
                trainingResource.getCatalogueId());
        onboard(trainingResource, provider, auth);
        onboardingValidation(trainingResource, provider);
        TrainingResourceBundle ret = genericResourceService.add(getResourceTypeName(), trainingResource);
        return ret;
    }

    private void onboard(TrainingResourceBundle trainingResource, ProviderBundle provider, Authentication auth) {
        String catalogueId = trainingResource.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            if (provider.getTemplateStatus().equals("approved template")) {
                trainingResource.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
                trainingResource.setActive(true);
            } else {
                trainingResource.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
            }
            trainingResource.setCatalogueId(this.catalogueId);
            commonMethods.createIdentifiers(trainingResource, getResourceTypeName(), false);
            trainingResource.setId(trainingResource.getIdentifiers().getOriginalId());
        } else {
            trainingResource.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
//            commonMethods.validateCatalogueId(catalogueId); //FIXME
            idCreator.validateId(trainingResource.getId());
            commonMethods.createIdentifiers(trainingResource, getResourceTypeName(), true);
        }
        trainingResource.setAuditState(Auditable.NOT_AUDITED);
    }

    private void onboardingValidation(TrainingResourceBundle trainingResource, ProviderBundle provider) {
        relationshipValidator.checkRelatedResourceIDsConsistency(trainingResource);
        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();
        if (!provider.getStatus().equals("approved")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Owner " +
                    "is not yet approved", provider.getId()), HttpStatus.CONFLICT);
        }
        if (provider.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource " +
                    "Template.", provider.getId()), HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public TrainingResourceBundle update(TrainingResourceBundle trainingResource, String comment, Authentication auth) {
        TrainingResourceBundle existing = get(trainingResource.getId(), trainingResource.getCatalogueId());
        // check if there are actual changes in the Training Resource
        if (trainingResource.equals(existing)) {
            return trainingResource;
        }
        trainingResource.markUpdate(auth, comment);
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
    public TrainingResourceBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        TrainingResourceBundle existing = get(id);
        existing.markOnboard(status, active, auth, null);

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

        existing.markActive(active, auth);
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
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", false); // A Draft Provider cannot have resources
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        if (providers.isEmpty()) {
            return new Browsing<>();
        }

        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(maxQuantity);
        filter.addFilter("published", false);
        filter.addFilter("owner", providers.stream().map(ProviderBundle::getId).toList());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
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
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        TrainingResourceBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public TrainingResourceBundle updateDraft(TrainingResourceBundle bundle, Authentication auth) {
        bundle.markUpdate(auth, null);
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
        if (provider.getTemplateStatus().equals("approved template")) {
            trainingResource.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
        } else {
            trainingResource.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
        }
        trainingResource = update(trainingResource, auth);

        return trainingResource;
    }
    //endregion
}
