/*
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
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

//TODO: REMOVE ANY LOGIC THAT RELATES WITH MODEL'S FIELDS (eg. name, users, HLE)
@org.springframework.stereotype.Service("providerTestManager")
public class ProviderTestManager extends gr.uoa.di.madgik.resourcecatalogue.manager.TestManager<NewProviderBundle>
        implements ProviderTestService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestManager.class);

    @Value("${catalogue.id}")
    private String catalogueId;

    private final GenericResourceService genericResourceService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ServiceBundleService serviceBundleService; //FIXME: do we need <?>
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final IdCreator idCreator;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SecurityService securityService;
    private final CatalogueService catalogueService;

    public ProviderTestManager(GenericResourceService genericResourceService,
                               RegistrationMailService registrationMailService,
                               VocabularyService vocabularyService,
                               ServiceBundleService serviceBundleService,
                               TrainingResourceService trainingResourceService,
                               InteroperabilityRecordService interoperabilityRecordService,
                               IdCreator idCreator,
                               ProviderResourcesCommonMethods commonMethods,
                               SecurityService securityService,
                               CatalogueService catalogueService) {
        super(genericResourceService, securityService, catalogueService);
        this.genericResourceService = genericResourceService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.securityService = securityService;
        this.catalogueService = catalogueService;
    }

    @Override
    protected String getResourceTypeName() {
        return "providertest";
    }

    @Override
    public NewProviderBundle add(NewProviderBundle bundle, Authentication auth) {
        onboard(bundle, auth);
        NewProviderBundle ret = genericResourceService.add(getResourceTypeName(), bundle);
//        registrationMailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null); //FIXME
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
    public NewProviderBundle update(NewProviderBundle bundle, String comment, Authentication auth) {
        NewProviderBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Provider
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(auth, comment);
        return update(bundle, auth);
    }

    @Override
    public NewProviderBundle update(NewProviderBundle bundle, Authentication auth) {
        NewProviderBundle existing = get(bundle.getId(), bundle.getCatalogueId()); //TODO: I don't like calling it twice
        try {
            NewProviderBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
            checkAndAddProviderToHLEVocabulary(bundle);
            sendEmailsAfterProviderUpdate(bundle, existing);
//            synchronizerService.syncUpdate(bundle.getProvider()); // TODO: remove this?
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: Do we need specific model validation? -> VocabularyValidationUtils -> Should it be transferred into catalogue lib?

    @Override
    public void delete(NewProviderBundle bundle) {
        String catalogueId = bundle.getCatalogueId();
        // block Public Provider deletion
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Provider");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.trace("User is attempting to delete the Provider with id '{}'", bundle.getId());
//        List<ServiceBundle> services =
//                serviceBundleService.getResourceBundles(catalogueId, provider.getId(), authentication).getResults();
//        if (services != null && !services.isEmpty()) {
//            services.forEach(s -> {
//                if (!s.getMetadata().isPublished()) {
//                    try {
//                        serviceBundleService.delete(s);
//                    } catch (ResourceNotFoundException e) {
//                        logger.error("Error deleting Service with ID '{}'", s.getId());
//                    }
//                }
//            });
//        }
//        List<TrainingResourceBundle> trainingResources =
//                trainingResourceService.getResourceBundles(catalogueId, provider.getId(), authentication).getResults();
//        if (trainingResources != null && !trainingResources.isEmpty()) {
//            trainingResources.forEach(s -> {
//                if (!s.getMetadata().isPublished()) {
//                    try {
//                        trainingResourceService.delete(s);
//                    } catch (ResourceNotFoundException e) {
//                        logger.error("Error deleting Training Resource with ID '{}'", s.getId());
//                    }
//                }
//            });
//        }
//        List<InteroperabilityRecordBundle> interoperabilityRecords =
//                interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, provider.getId(), authentication).getResults();
//        if (interoperabilityRecords != null && !interoperabilityRecords.isEmpty()) {
//            interoperabilityRecords.forEach(s -> {
//                if (!s.getMetadata().isPublished()) {
//                    try {
//                        interoperabilityRecordService.delete(s);
//                    } catch (ResourceNotFoundException e) {
//                        logger.error("Error deleting Interoperability Record with ID '{}'", s.getId());
//                    }
//                }
//            });
//        }
        logger.debug("Deleting Provider: {} and all his Resources", bundle.getId());

        genericResourceService.delete(getResourceTypeName(), bundle.getId());

        // TODO: move to aspect
//        registrationMailService.notifyProviderAdminsForProviderDeletion(bundle);

//        synchronizerService.syncDelete(bundle.getProvider());
    }

    @Override
    public NewProviderBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        NewProviderBundle existing = get(id);
        existing.markOnboard(status, active, auth, null);

        if (status.equals("approved")) {
            checkAndAddProviderToHLEVocabulary(existing);
        }

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
            throw new ValidationException(String.format("You cannot activate this Provider, because it's Inactive with status = [%s]",
                    existing.getStatus()));
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
        NewProviderBundle existing = get(id);
//        commonMethods.suspensionValidation(existing, catalogueId, id, suspend, auth); //FIXME

        existing.markSuspend(suspend, auth);

        // Suspend Provider's resources
        List<ServiceBundle> services = serviceBundleService.getResourceBundles(catalogueId, id, auth).getResults();
        List<TrainingResourceBundle> trainingResources = trainingResourceService.getResourceBundles(catalogueId, id, auth).getResults();
        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, id, auth).getResults();

//        if (services != null && !services.isEmpty()) {
//            for (ServiceBundle serviceBundle : services) {
//                serviceBundleService.suspend(serviceBundle.getId(), catalogueId, suspend, auth);
//            }
//        }
//        if (trainingResources != null && !trainingResources.isEmpty()) {
//            for (TrainingResourceBundle trainingResourceBundle : trainingResources) {
//                trainingResourceService.suspend(trainingResourceBundle.getId(), catalogueId, suspend, auth);
//            }
//        }
//        if (interoperabilityRecords != null && !interoperabilityRecords.isEmpty()) {
//            for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecords) {
//                interoperabilityRecordService.suspend(interoperabilityRecordBundle.getId(), catalogueId, suspend, auth);
//            }
//        }

        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NewProviderBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        NewProviderBundle existing = get(id, catalogueId);
        existing.markAudit(comment, actionType, auth);

        // send notification emails to Provider Admins
//        registrationMailService.notifyProviderAdminsForBundleAuditing(existing, existing.getProvider().get("users")); //FIXME

        logger.info("Audited Provider '{}' with [actionType: {}]", existing.getId(), actionType);

        try {
            genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return existing;
    }

    @Override
    public List<LoggingInfo> getLoggingInfoHistory(NewProviderBundle bundle) {
        return ProviderTestService.super.getLoggingInfoHistory(bundle);
    }

    @Override
    public Paging<NewProviderBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000);
        ff.addFilter("status", "approved");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);

        Browsing<NewProviderBundle> providersBrowsing = getAll(ff, auth);
        List<NewProviderBundle> providersToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(auditingInterval)
                .toEpochSecond();

        for (NewProviderBundle bundle : providersBrowsing.getResults()) {
            LoggingInfo auditInfo = bundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include providers that have never been audited
                providersToBeAudited.add(bundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include providers that were last audited before the threshold
                        providersToBeAudited.add(bundle);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(providersToBeAudited);

        // Limit the list to the requested quantity
        if (providersToBeAudited.size() > quantity) {
            providersToBeAudited = providersToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(providersToBeAudited.size(), 0, providersToBeAudited.size(), providersToBeAudited,
                providersBrowsing.getFacets());
    }

    @Override
    public void requestProviderDeletion(String providerId, Authentication auth) {
        NewProviderBundle provider = genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", providerId),
                new SearchService.KeyValue("published", "false"));

        List<String> userEmails = extractEmails(provider);
        for (String email : userEmails) {
            if (email.equalsIgnoreCase(AuthenticationInfo.getEmail(auth).toLowerCase())) {
//                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth)); //FIXME
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
            services.addAll(serviceBundleService.getResourceBundles(providerBundle.getCatalogueId(),
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

    private void sendEmailsAfterProviderUpdate(NewProviderBundle updatedProvider, NewProviderBundle existingProvider) {
        sendEmailsForAdminDifferences(updatedProvider, existingProvider);
        sendEmailsForAuditInfo(updatedProvider);
    }

    private void sendEmailsForAdminDifferences(NewProviderBundle updatedProvider, NewProviderBundle existingProvider) {
        List<List<String>> differences = calculateDifferences(updatedProvider, existingProvider);
        sendEmailsToProviderAdmins(differences);
    }

    private List<List<String>> calculateDifferences(NewProviderBundle updatedProvider, NewProviderBundle existingProvider) {
        List<String> existingAdmins = extractEmails(existingProvider);
        List<String> newAdmins = extractEmails(updatedProvider);
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);

        List<List<String>> differences = new ArrayList<>();
        differences.add(adminsAdded);
        differences.add(adminsDeleted);
        return differences;
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

    private void sendEmailsToProviderAdmins(List<List<String>> differences) {
        if (!differences.getFirst().isEmpty()) {
//            registrationMailService.sendEmailsToNewlyAddedProviderAdmins(updatedProvider, adminsAdded); //TODO: fix & enable
        }
        if (!differences.getLast().isEmpty()) {
//            registrationMailService.sendEmailsToNewlyDeletedProviderAdmins(existingProvider, adminsDeleted); //TODO: fix & enable
        }
    }

    private void sendEmailsForAuditInfo(NewProviderBundle updatedProvider) {
        if (updatedProvider.getLatestAuditInfo() != null &&
                LoggingInfo.ActionType.INVALID.getKey().equals(updatedProvider.getLatestAuditInfo().getActionType())) {
            long latestAudit = Long.parseLong(updatedProvider.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(updatedProvider.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate) {
//                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(bundle); //TODO: fix & enable
            }
        }
    }

    //TODO: call on update and verify
    private void checkAndAddProviderToHLEVocabulary(NewProviderBundle bundle) {
        boolean legalEntity = switch (bundle.getProvider().get("legalEntity")) { // TODO: field type (in model) should be 'boolean', not radio with "true"/"false" values.
            case Boolean value -> value;
            case String str -> Boolean.parseBoolean(str);
            default -> throw new ValidationException("Error in field 'legalEntity', should be boolean.");
        };
        if (bundle.getStatus().toLowerCase().contains("approved") && legalEntity) {
            addUpdateProviderHLEVocabulary(bundle);
        }
    }

    private void addUpdateProviderHLEVocabulary(NewProviderBundle bundle) {
        String hleId = createProviderHleId(bundle);
        Vocabulary hle = vocabularyService.get();
        if (hle != null) {
            if (!hle.getName().equals(bundle.getProvider().get("name"))) {
                hle.setName(bundle.getProvider().get("name").toString());
                vocabularyService.update(hle, null);
            }
        } else { // create new entry
            hle = new Vocabulary();
            hle.setId(hleId);
            hle.setName(bundle.getProvider().get("name").toString());
            hle.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
            hle.setExtras(new HashMap<>() {{
                put("catalogueId", bundle.getCatalogueId());
            }});
            logger.info("Creating a new Hosting Legal Entity Vocabulary with id: [{}] and name: [{}]",
                    hle.getId(), hle.getName());
            vocabularyService.add(hle, null);
        }
    }

    private String createProviderHleId(NewProviderBundle bundle) {
        return "%s-%s".formatted(
                Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey().toLowerCase().replace(" ", "_"),
                bundle.getId()
        );
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

//        registrationMailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null); //FIXME
        return bundle;
    }
}
