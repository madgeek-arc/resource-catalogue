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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.TriggersAspects;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderCascadeLifecycleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

//TODO: REMOVE ANY LOGIC THAT RELATES WITH MODEL'S FIELDS (eg. name, users, HLE)
@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends gr.uoa.di.madgik.resourcecatalogue.manager.TestManager<NewProviderBundle>
        implements ProviderService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManager.class);

    @Value("${catalogue.id}")
    private String catalogueId;

    private final GenericResourceService genericResourceService;
    private final EmailService emailService;
    private final VocabularyService vocabularyService;
    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final IdCreator idCreator;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SecurityService securityService;
    private final CatalogueService catalogueService;
    private final ProviderCascadeLifecycleService cascadeLifecycleService;

    public ProviderManager(GenericResourceService genericResourceService,
                               EmailService emailService,
                               VocabularyService vocabularyService,
                               ServiceService serviceService,
                               TrainingResourceService trainingResourceService,
                               InteroperabilityRecordService interoperabilityRecordService,
                               IdCreator idCreator,
                               ProviderResourcesCommonMethods commonMethods,
                               SecurityService securityService,
                               CatalogueService catalogueService,
                               ProviderCascadeLifecycleService cascadeLifecycleService) {
        super(genericResourceService, securityService, catalogueService);
        this.genericResourceService = genericResourceService;
        this.emailService = emailService;
        this.vocabularyService = vocabularyService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.securityService = securityService;
        this.catalogueService = catalogueService;
        this.cascadeLifecycleService = cascadeLifecycleService;
    }

    @Override
    protected String getResourceTypeName() {
        return "providertest";
    }

    @Override
    public NewProviderBundle add(NewProviderBundle bundle, Authentication auth) {
        onboard(bundle, auth);
        NewProviderBundle ret = genericResourceService.add(getResourceTypeName(), bundle);
//        emailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null); //FIXME
//        synchronizerService.syncAdd(bundle.getProvider()); //TODO: remove this?
        return ret;
    }

    private void onboard(NewProviderBundle bundle, Authentication auth) {
        String catalogueId = bundle.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            bundle.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
            bundle.setCatalogueId(this.catalogueId); //TODO: how we proceed with instance's catalogue ID
            bundle.setTemplateStatus(vocabularyService.get("no template status").getId());
            //TODO: make sure we need to create our own IDs instead of users giving them
            bundle.setId(idCreator.generate(getResourceTypeName()));
//            commonMethods.createIdentifiers(bundle, getResourceTypeName(), false); //TODO: fix and enable
        } else {
            bundle.setCatalogueId(catalogueId);
            bundle.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
            commonMethods.checkCatalogueIdConsistency(bundle, catalogueId); //TODO: test me
            bundle.setTemplateStatus(vocabularyService.get("approved template").getId());
            idCreator.validateId(bundle.getId());
//            commonMethods.createIdentifiers(bundle, getResourceTypeName(), true); //TODO: fix and enable
        }

        commonMethods.addAuthenticatedUser(bundle.getProvider(), auth);
        bundle.setAuditState(Auditable.NOT_AUDITED);
    }

    @Override
    @TriggersAspects({"HostingLegalEntityVocabularyUpdate", "AfterProviderUpdateEmails"})
    public NewProviderBundle update(NewProviderBundle bundle, String comment, Authentication auth) {
        NewProviderBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Provider
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(auth, comment);
        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
//            synchronizerService.syncUpdate(bundle.getProvider()); // TODO: remove this?
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: Do we need specific model validation? -> VocabularyValidationUtils -> Should it be transferred into catalogue lib?

    @Override
    @Transactional // if deleteAllRelatedResources() fails, this should also fail
    @TriggersAspects({"AfterProviderDeletionEmails"})
    public void delete(NewProviderBundle bundle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // block Public Provider deletion
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Provider");
        }

        logger.info("Deleting Provider: {} and all its Resources", bundle.getId());
//        cascadeLifecycleService.deleteAllRelatedResources(bundle, auth); //FIXME
        genericResourceService.delete(getResourceTypeName(), bundle.getId());

//        synchronizerService.syncDelete(bundle.getProvider()); //TODO: remove this?
    }

    @Override
    @TriggersAspects({"HostingLegalEntityVocabularyUpdate"})
    public NewProviderBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        NewProviderBundle existing = get(id);
        existing.markOnboard(status, active, auth, null);

        logger.info("Verifying Provider: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NewProviderBundle setActive(String id, Boolean active, Authentication auth) {
        NewProviderBundle existing = get(id);

        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Provider, because it is not yet approved.");
        }

        existing.markActive(active, auth);
//        activateProviderResources(existingProvider.getId(), active, auth); //FIXME
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasAdminAcceptedTerms(String id, Authentication auth) {
        NewProviderBundle bundle = get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
        String userEmail = AuthenticationInfo.getEmail(auth).toLowerCase();

        List<String> providerAdmins = extractEmails(bundle);
        List<String> acceptedTerms = bundle.getMetadata().getTerms();

        if (acceptedTerms == null || acceptedTerms.isEmpty()) {
            return !providerAdmins.contains(userEmail); // false -> show modal, true -> no modal
        }

        return !providerAdmins.contains(userEmail) || acceptedTerms.contains(userEmail); // Show or not modal
    }

    @Override
    public void adminAcceptedTerms(String id, Authentication auth) {
        NewProviderBundle bundle = get(id);
        String userEmail = AuthenticationInfo.getEmail(auth);

        List<String> existingTerms = bundle.getMetadata().getTerms();
        if (existingTerms == null) {
            existingTerms = new ArrayList<>();
        }

        if (!existingTerms.contains(userEmail)) {
            existingTerms.add(userEmail);
            bundle.getMetadata().setTerms(existingTerms);

            try {
                genericResourceService.update(getResourceTypeName(), id, bundle);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.info("Could not update terms for Provider with id: '{}'", id);
            } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public NewProviderBundle setSuspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        NewProviderBundle bundle = get(id, catalogueId);
//        commonMethods.suspensionValidation(existing, catalogueId, id, suspend, auth); //FIXME

        logger.info("Suspending Provider: {} and all its Resources", bundle.getId());
        bundle.markSuspend(suspend, auth);
//        cascadeLifecycleService.suspendAllRelatedResources(bundle, auth); //FIXME

        try {
            return genericResourceService.update(getResourceTypeName(), id, bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void requestProviderDeletion(String providerId, Authentication auth) {
        NewProviderBundle provider = genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", providerId),
                new SearchService.KeyValue("published", "false"));

        List<String> userEmails = extractEmails(provider);
        for (String email : userEmails) {
            if (email.equalsIgnoreCase(AuthenticationInfo.getEmail(auth).toLowerCase())) {
//                emailService.informPortalAdminsForProviderDeletion(provider, User.of(auth)); //FIXME
            }
        }
    }

    @Override
    public String determineHostingLegalEntity(String providerName) {
        List<Vocabulary> hostingLegalEntityList = vocabularyService.getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        for (Vocabulary hle : hostingLegalEntityList) {
            if (hle.getName().equalsIgnoreCase(providerName)) {
                return hle.getId();
            }
        }
        return null;
    }

    @Override
    public List<MapValues<CatalogueValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("hosting_legal_entity", hle);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        List<MapValues<CatalogueValue>> mapValuesList = new ArrayList<>();
        List<NewProviderBundle> providers = getAll(ff, auth).getResults();
        List<ServiceBundle> services = new ArrayList<>();
        List<TrainingResourceBundle> trainingResources = new ArrayList<>();
        List<InteroperabilityRecordBundle> interoperabilityRecords = new ArrayList<>();
        createMapValuesForHLE(providers, "provider", mapValuesList);
        for (NewProviderBundle providerBundle : providers) {
            services.addAll(serviceService.getResourceBundles(providerBundle.getCatalogueId(),
                    providerBundle.getId(), auth).getResults());
            trainingResources.addAll(trainingResourceService.getResourceBundles(providerBundle.getCatalogueId(),
                    providerBundle.getId(), auth).getResults());
            interoperabilityRecords.addAll(interoperabilityRecordService.getInteroperabilityRecordBundles(
                    providerBundle.getCatalogueId(), providerBundle.getId(), auth).getResults());
        }
        createMapValuesForHLE(services, "service", mapValuesList);
        createMapValuesForHLE(trainingResources, "training_resource", mapValuesList);
        createMapValuesForHLE(interoperabilityRecords, "interoperability_record", mapValuesList);
        return mapValuesList;
    }

    private void createMapValuesForHLE(List<?> resources, String resourceType,
                                       List<MapValues<CatalogueValue>> mapValuesList) {
        MapValues<CatalogueValue> mapValues = new MapValues<>();
        mapValues.setKey(resourceType);
        List<CatalogueValue> valueList = new ArrayList<>();
        for (Object obj : resources) {
            CatalogueValue value = new CatalogueValue();
            switch (resourceType) {
                case "provider":
                    NewProviderBundle providerBundle = (NewProviderBundle) obj;
                    value.setId(providerBundle.getId());
                    value.setName(providerBundle.getProvider().get("name").toString());
                    value.setCatalogue(providerBundle.getCatalogueId());
                    break;
                case "service":
                    ServiceBundle serviceBundle = (ServiceBundle) obj;
                    value.setId(serviceBundle.getId());
                    value.setName(serviceBundle.getService().getName());
                    value.setCatalogue(serviceBundle.getService().getCatalogueId());
                    break;
                case "training_resource":
                    TrainingResourceBundle trainingResourceBundle = (TrainingResourceBundle) obj;
                    value.setId(trainingResourceBundle.getId());
                    value.setName(trainingResourceBundle.getTrainingResource().getTitle());
                    value.setCatalogue(trainingResourceBundle.getTrainingResource().getCatalogueId());
                    break;
                case "interoperability_record":
                    InteroperabilityRecordBundle interoperabilityRecordBundle = (InteroperabilityRecordBundle) obj;
                    value.setId(interoperabilityRecordBundle.getId());
                    value.setName(interoperabilityRecordBundle.getInteroperabilityRecord().getTitle());
                    value.setCatalogue(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
                    break;
                default:
                    break;
            }
            valueList.add(value);
        }
        mapValues.setValues(valueList);
        mapValuesList.add(mapValues);
    }

    private List<String> extractEmails(NewProviderBundle providerBundle) {
        List<String> emails = new ArrayList<>();

        Object usersObj = providerBundle.getProvider().get("users"); //TODO: how to enforce that users will be always in the model
        if (usersObj instanceof Collection<?>) {
            for (Object obj : (Collection<?>) usersObj) {
                if (obj instanceof User user) {
                    emails.add(user.getEmail().toLowerCase());
                }
            }
        }
        return emails;
    }

    public Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> getProviderIdToNameMap(String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allProviders = new ArrayList<>();
        // fetch catalogueId related non-public Providers
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedProviders = getAll(createFacetFilter(catalogueId, false))
                .getResults()
                .stream()
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
                        c.getProvider().get("id").toString(), c.getProvider().get("name").toString())
                )
                .toList();
        // fetch non-catalogueId related public Providers
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicProviders = getAll(createFacetFilter(catalogueId, true)).getResults()
                .stream()
                .filter(c -> !c.getProvider().get("catalogueId").equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
                        c.getProvider().get("id").toString(), c.getProvider().get("name").toString())
                )
                .toList();

        allProviders.addAll(catalogueRelatedProviders);
        allProviders.addAll(publicProviders);
        ret.put("PROVIDERS_VOC", allProviders);
        return ret;
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000);
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        ff.addFilter("draft", false);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        return ff;
    }

    //region Drafts
    @Override
    public NewProviderBundle addDraft(NewProviderBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setId(idCreator.generate(getResourceTypeName()));
        bundle.setCatalogueId(catalogueId);
//        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false); //FIXME
        commonMethods.addAuthenticatedUser(bundle.getProvider(), auth);

        NewProviderBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public NewProviderBundle updateDraft(NewProviderBundle bundle, Authentication auth) {
        bundle.markUpdate(auth, null);
        try {
            NewProviderBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(NewProviderBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public NewProviderBundle finalizeDraft(NewProviderBundle bundle, Authentication auth) {
        bundle.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
        bundle.setTemplateStatus(vocabularyService.get("no template status").getId());

        bundle = update(bundle, auth);

//        emailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null); //FIXME
        return bundle;
    }
    //endregion
}
