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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateMerilScientificDomains;
import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;

//TODO: REMOVE PUBLISHED FALSE IF IDS ARE UNIQUE
@org.springframework.stereotype.Service("providerTestManager")
public class ProviderTestManager implements ProviderTestService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestManager.class);
    private final String resourceTypeName = "providertest";

    private final GenericResourceService genericResourceService;
    private final RegistrationMailService registrationMailService;
    private final SearchService searchService;
    private final PublicProviderService publicProviderService;
    private final VocabularyService vocabularyService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService; //FIXME: do we need <?>
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final IdCreator idCreator;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SecurityService securityService;
//    private final SynchronizerService<LinkedHashMap<String, Object>> synchronizerService;

    public ProviderTestManager(GenericResourceService genericResourceService,
                               SearchService searchService,
                               RegistrationMailService registrationMailService,
                               PublicProviderService publicProviderService,
                               VocabularyService vocabularyService,
                               ServiceBundleService<ServiceBundle> serviceBundleService,
                               TrainingResourceService trainingResourceService,
                               InteroperabilityRecordService interoperabilityRecordService,
                               IdCreator idCreator,
                               ProviderResourcesCommonMethods commonMethods,
                               SecurityService securityService)
//                               SynchronizerService<LinkedHashMap<String, Object>> synchronizerService)
                               {
        this.genericResourceService = genericResourceService;
        this.searchService = searchService;
        this.registrationMailService = registrationMailService;
        this.publicProviderService = publicProviderService;
        this.vocabularyService = vocabularyService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.securityService = securityService;
//        this.synchronizerService = synchronizerService;
    }

    @Override
    public NewProviderBundle get(String id) {
        return genericResourceService.get(resourceTypeName,
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
        //TODO: do we need this?
//        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
//        if (catalogueBundle == null) {
//            throw new CatalogueResourceNotFoundException(
//                    String.format("Could not find catalogue with id: %s", catalogueId));
//        }
//        if (!providerBundle.getProvider().getCatalogueId().equals(catalogueId)) {
//            throw new ResourceException(String.format("Provider with id [%s] does not belong to the catalogue with id [%s]",
//                    providerId, catalogueId), HttpStatus.CONFLICT);
//        }
        //TODO: moved on Controller's PostAuth -> check that it works
//        if (auth != null && auth.isAuthenticated()) {
//            User user = User.of(auth);
//            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
//            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
//                    securityService.userHasAdminAccess(user, providerId)) {
//                return providerBundle;
//            }
//        }
//        // else return the Provider ONLY if he is active
//        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
//            return providerBundle;
//        }
//        throw new InsufficientAuthenticationException("You cannot view the specific Provider");
    }

    @Override
    public Browsing<NewProviderBundle> getAll(FacetFilter ff, Authentication auth) {
        boolean authenticated = auth != null && auth.isAuthenticated();
        if (authenticated) {
            if (securityService.hasPortalAdminRole(auth)) {
                return getAll(ff);
            }
            if (securityService.hasRole(auth, "ROLE_PROVIDER")) {
                ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
                return getAll(ff);
            }
        }
        ff.addFilter("status", "approved provider");
        ff.addFilter("active", true);
        return getAll(ff);
    }

    @Override
    public Browsing<NewProviderBundle> getAll(FacetFilter filter) {
        return genericResourceService.getResults(filter);
    }

    @Override
    public Browsing<NewProviderBundle> getMy(FacetFilter ff, Authentication auth) {
        if (ff == null) {
            ff = new FacetFilter();
            ff.setResourceType(resourceTypeName);
            ff.setQuantity(10000);
            ff.addFilter("published", false);
            ff.addFilter("draft", false);
        }
        if (!ff.getFilter().containsKey("published")) {
            ff.addFilter("published", false);
        }
        ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    @Override
    public NewProviderBundle add(NewProviderBundle bundle, Authentication auth) {
        return add(bundle, null, auth);
    }

    @Override
    public NewProviderBundle add(NewProviderBundle bundle, String catalogueId, Authentication auth) {
        onboard(bundle, catalogueId, auth);
        NewProviderBundle ret = genericResourceService.add(resourceTypeName, bundle);
//        registrationMailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null); //FIXME
//        synchronizerService.syncAdd(bundle.getProvider()); //TODO: remove this?
        return ret;
    }

    private void onboard(NewProviderBundle bundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.isEmpty()) {
            bundle.markOnboard(vocabularyService.get("pending provider").getId(), false, auth, null);
            bundle.setCatalogueId(null); //TODO: how we proceed with instance's catalogue ID
            bundle.setTemplateStatus(vocabularyService.get("no template status").getId());
            //TODO: make sure we need to create our own IDs instead of users giving them
            bundle.getProvider().put("id", idCreator.generate(resourceTypeName));
//            commonMethods.createIdentifiers(bundle, resourceTypeName, false); //TODO: fix and enable
        } else {
            bundle.markOnboard(vocabularyService.get("approved provider").getId(), true, auth, null);
            commonMethods.checkCatalogueIdConsistency(bundle, catalogueId);
            bundle.setTemplateStatus(vocabularyService.get("approved template").getId());
            idCreator.validateId(bundle.getProvider().get("id").toString());
//            commonMethods.createIdentifiers(bundle, resourceTypeName, true); //TODO: fix and enable
        }

        commonMethods.addAuthenticatedUser(bundle.getProvider(), auth);
        bundle.setAuditState(Auditable.NOT_AUDITED);
    }

    @Override
    public NewProviderBundle update(NewProviderBundle bundle, String catalogueId, String comment, Authentication auth) {
        NewProviderBundle existing = get(bundle.getProvider().get("id").toString());
        // check if there are actual changes in the Provider
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(auth, comment);
        return update(bundle, auth);
    }

    @Override
    public NewProviderBundle update(NewProviderBundle bundle, Authentication auth) {
        NewProviderBundle existing = get(bundle.getProvider().get("id").toString()); //TODO: I don't like calling it twice
        validate(bundle);

        try {
            NewProviderBundle ret = genericResourceService.update(resourceTypeName,
                    bundle.getProvider().get("id").toString(), bundle);

            checkAndAddProviderToHLEVocabulary(bundle);
            sendEmailsAfterProviderUpdate(bundle, existing);
//            synchronizerService.syncUpdate(bundle.getProvider()); // TODO: remove this?
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendEmailsAfterProviderUpdate(NewProviderBundle updatedProvider, NewProviderBundle existingProvider) {
        // Send emails to newly added or deleted Admins
        adminDifferences(updatedProvider, existingProvider);

        // send notification emails to Portal Admins
        if (updatedProvider.getLatestAuditInfo() != null &&
                updatedProvider.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            long latestAudit = Long.parseLong(updatedProvider.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(updatedProvider.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate) {
//                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(bundle); //TODO: fix & enable
            }
        }
    }

    public void adminDifferences(NewProviderBundle updatedProvider, NewProviderBundle existingProvider) {
        List<String> existingAdmins = extractEmails(existingProvider);
        List<String> newAdmins = extractEmails(updatedProvider);

        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        if (!adminsAdded.isEmpty()) {
//            registrationMailService.sendEmailsToNewlyAddedProviderAdmins(updatedProvider, adminsAdded); //TODO: fix & enable
        }

        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);

        if (!adminsDeleted.isEmpty()) {
//            registrationMailService.sendEmailsToNewlyDeletedProviderAdmins(existingProvider, adminsDeleted); //TODO: fix & enable
        }
    }

    private List<String> extractEmails(NewProviderBundle providerBundle) {
        List<String> emails = new ArrayList<>();

        Object usersObj = providerBundle.getProvider().get("users");
        if (usersObj instanceof Collection<?>) {
            for (Object obj : (Collection<?>) usersObj) {
                if (obj instanceof User user) {
                    emails.add(user.getEmail().toLowerCase());
                }
            }
        }
        return emails;
    }

    @Override
    public void delete(NewProviderBundle resourceId) {
        //TODO: fill method
    }

    @Override
    public NewProviderBundle verify(String id, String status, Boolean active, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle publish(String id, Boolean active, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public boolean hasAdminAcceptedTerms(FacetFilter ff, Authentication auth) {
        //TODO: fill method
        return false;
    }

    @Override
    public void adminAcceptedTerms(FacetFilter ff, Authentication auth) {
        //TODO: fill method
    }

    @Override
    public NewProviderBundle suspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public List<LoggingInfo> getLoggingInfoHistory(NewProviderBundle bundle) {
        return ProviderTestService.super.getLoggingInfoHistory(bundle);
    }

    @Override
    public Paging<NewProviderBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle get(String id, String catalogueId) {
        //TODO: fill method
        return null;
    }

    @Override
    public String createId(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return "";
    }

    @Override
    public NewProviderBundle save(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return null;
    }

    @Override
    public Map<String, List<NewProviderBundle>> getBy(String field) {
        //TODO: fill method
        return Map.of();
    }

    @Override
    public List<NewProviderBundle> getSome(String... ids) {
        //TODO: fill method
        return List.of();
    }

    @Override
    public NewProviderBundle get(SearchService.KeyValue... keyValues) {
        return genericResourceService.get(resourceTypeName, keyValues);
    }

    @Override
    public List<NewProviderBundle> delAll() {
        //TODO: fill method
        return List.of();
    }

    @Override
    public NewProviderBundle validate(NewProviderBundle bundle) {
        logger.debug("Validating Provider with id: '{}'", bundle.getProvider().get("id"));
        return genericResourceService.validate(bundle.getProvider());
    }

    @Override
    public Resource getResource(String id) {
        //TODO: fill method
        return null;
    }

    @Override
    public Resource getResource(String id, String catalogueId) {
        //TODO: fill method
        return null;
    }

    @Override
    public boolean exists(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return false;
    }

    @Override
    public boolean exists(String id) {
        //TODO: fill method
        return false;
    }

    //TODO: move to PublicController
    @Override
    public NewProviderBundle createPublicProvider(NewProviderBundle bundle, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public void requestProviderDeletion(String providerId, Authentication auth) {
        ProviderBundle provider = genericResourceService.get(resourceTypeName,
                new SearchService.KeyValue("resource_internal_id", providerId),
                new SearchService.KeyValue("published", "false"));
        for (User user : provider.getProvider().getUsers()) {
            if (user.getEmail().equalsIgnoreCase(AuthenticationInfo.getEmail(auth).toLowerCase())) {
                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth));
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
        List<MapValues<CatalogueValue>> mapValuesList = new ArrayList<>();
        List<NewProviderBundle> providers = getAll(ff, auth).getResults();
        List<ServiceBundle> services = new ArrayList<>();
        List<TrainingResourceBundle> trainingResources = new ArrayList<>();
        List<InteroperabilityRecordBundle> interoperabilityRecords = new ArrayList<>();
        createMapValuesForHLE(providers, "provider", mapValuesList);
        for (NewProviderBundle providerBundle : providers) {
            services.addAll(serviceBundleService.getResourceBundles(providerBundle.getCatalogueId(),
                    providerBundle.getProvider().get("id").toString(), auth).getResults());
            trainingResources.addAll(trainingResourceService.getResourceBundles(providerBundle.getCatalogueId(),
                    providerBundle.getProvider().get("id").toString(), auth).getResults());
            interoperabilityRecords.addAll(interoperabilityRecordService.getInteroperabilityRecordBundles(
                    providerBundle.getCatalogueId(), providerBundle.getProvider().get("id").toString(), auth).getResults());
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
                    value.setId(providerBundle.getProvider().get("id").toString());
                    value.setName(providerBundle.getProvider().get("name").toString());
                    value.setCatalogue(providerBundle.getProvider().get("catalogue").toString());
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

    //TODO: call on update and verify
    private void checkAndAddProviderToHLEVocabulary(NewProviderBundle bundle) {
        boolean legalEntity = (Boolean) bundle.getProvider().get("legalEntity");
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
                bundle.getProvider().get("id").toString()
        );
    }
}
