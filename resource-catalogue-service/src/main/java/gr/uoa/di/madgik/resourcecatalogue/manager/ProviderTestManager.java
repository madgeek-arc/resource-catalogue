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
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.VersionService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateMerilScientificDomains;
import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;

@org.springframework.stereotype.Service("providerTestManager")
public class ProviderTestManager implements ProviderTestService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestManager.class);
    private final String resourceTypeName = "providertest";
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final DeployableServiceService deployableServiceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final PublicServiceService publicServiceManager;
    private final PublicProviderService publicProviderService;
    private final PublicTrainingResourceService publicTrainingResourceManager;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordManager;
    private final SecurityService securityService;
    private final IdCreator idCreator;
    private final EventService eventService;
    private final RegistrationMailService registrationMailService;
    private final VersionService versionService;
    private final VocabularyService vocabularyService;
    private final CatalogueService catalogueService;
    private final SynchronizerService<Provider> synchronizerService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicDeployableServiceService publicDeployableServiceService;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ProviderTestManager(@Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                               @Lazy SecurityService securityService,
                               @Lazy RegistrationMailService registrationMailService, IdCreator idCreator,
                               EventService eventService, VersionService versionService,
                               VocabularyService vocabularyService,
                               @Qualifier("providerSync") SynchronizerService<Provider> synchronizerService,
                               @Lazy ProviderResourcesCommonMethods commonMethods,
                               CatalogueService catalogueService,
                               @Lazy PublicServiceService publicServiceManager,
                               @Lazy PublicProviderService publicProviderService,
                               @Lazy TrainingResourceService trainingResourceService,
                               @Lazy DeployableServiceService deployableServiceService,
                               @Lazy InteroperabilityRecordService interoperabilityRecordService,
                               @Lazy PublicTrainingResourceService publicTrainingResourceManager,
                               @Lazy PublicInteroperabilityRecordService publicInteroperabilityRecordManager,
                               @Lazy PublicDeployableServiceService publicDeployableServiceService,
                               GenericResourceService genericResourceService) {
        this.serviceBundleService = serviceBundleService;
        this.securityService = securityService;
        this.idCreator = idCreator;
        this.eventService = eventService;
        this.registrationMailService = registrationMailService;
        this.versionService = versionService;
        this.vocabularyService = vocabularyService;
        this.synchronizerService = synchronizerService;
        this.commonMethods = commonMethods;
        this.catalogueService = catalogueService;
        this.publicServiceManager = publicServiceManager;
        this.publicProviderService = publicProviderService;
        this.trainingResourceService = trainingResourceService;
        this.deployableServiceService = deployableServiceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicDeployableServiceService = publicDeployableServiceService;
        this.genericResourceService = genericResourceService;
    }


    //    @Override
//    public String getResourceTypeName() {
//        return "provider";
//    }
//
//    @Override
//    public ProviderBundle add(ProviderBundle provider, Authentication authentication) {
//        return add(provider, null, authentication);
//    }
//
//    @Override
//    public ProviderBundle add(ProviderBundle provider, String catalogueId, Authentication auth) {
//        logger.trace("Attempting to add a new Provider: {} on Catalogue: '{}'", provider, catalogueId);
//
//        provider = onboard(provider, catalogueId, auth);
//
//        commonMethods.addAuthenticatedUser(provider.getProvider(), auth);
//        validate(provider);
//        provider.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
//
//        ProviderBundle ret;
//        ret = super.add(provider, null);
//        logger.debug("Adding Provider: {} of Catalogue: '{}'", provider, catalogueId);
//
//        registrationMailService.sendEmailsToNewlyAddedProviderAdmins(provider, null);
//
//        synchronizerService.syncAdd(provider.getProvider());
//
//        return ret;
//    }
//
//    @Override
//    public ProviderBundle update(ProviderBundle provider, String comment, Authentication auth) {
//        return update(provider, provider.getProvider().getCatalogueId(), comment, auth);
//    }
//
//    @Override
//    public ProviderBundle update(ProviderBundle providerBundle, String catalogueId, String comment, Authentication auth) {
//        logger.trace("Attempting to update the Provider with id '{}' of the Catalogue '{}'", providerBundle, providerBundle.getProvider().getCatalogueId());
//
//        ProviderBundle ret = ObjectUtils.clone(providerBundle);
//        Resource existingResource = getResource(ret.getId(), ret.getProvider().getCatalogueId(), false);
//        ProviderBundle existingProvider = deserialize(existingResource);
//        // check if there are actual changes in the Provider
//        if (ret.getTemplateStatus().equals(existingProvider.getTemplateStatus()) && ret.getProvider().equals(existingProvider.getProvider())) {
//            if (ret.isSuspended() == existingProvider.isSuspended()) {
//                return ret;
//            }
//        }
//
//        if (catalogueId == null || catalogueId.isEmpty()) {
//            ret.getProvider().setCatalogueId(this.catalogueId);
//        } else {
//            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
//        }
//
//        // block Public Provider update
//        if (ret.getMetadata().isPublished()) {
//            throw new ValidationException("You cannot directly update a Public Provider");
//        }
//
//        validate(ret);
//        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
//        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
//        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
//                LoggingInfo.ActionType.UPDATED.getKey(), comment);
//        loggingInfoList.add(loggingInfo);
//        ret.setLoggingInfo(loggingInfoList);
//
//        // latestLoggingInfo
//        ret.setLatestUpdateInfo(loggingInfo);
//        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
//        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));
//
//        // block catalogueId updates from Provider Admins
//        if (!securityService.hasRole(auth, "ROLE_ADMIN") && !existingProvider.getProvider().getCatalogueId().equals(ret.getProvider().getCatalogueId())) {
//            throw new ValidationException("You cannot change catalogueId");
//        }
//        ret.setIdentifiers(existingProvider.getIdentifiers());
//        ret.setActive(existingProvider.isActive());
//        ret.setStatus(existingProvider.getStatus());
//        ret.setSuspended(existingProvider.isSuspended());
//        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));
//        existingResource.setPayload(serialize(ret));
//        existingResource.setResourceType(getResourceType());
//        resourceService.updateResource(existingResource);
//        logger.debug("Updating Provider: {} of Catalogue: {}", ret, ret.getProvider().getCatalogueId());
//
//        // check if Provider has become a Legal Entity
//        checkAndAddProviderToHLEVocabulary(ret);
//
//        // Send emails to newly added or deleted Admins
//        adminDifferences(ret, existingProvider);
//
//        // send notification emails to Portal Admins
//        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
//            long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
//            long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
//            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(ret);
//            }
//        }
//
//        synchronizerService.syncUpdate(ret.getProvider());
//
//        return ret;
//    }
//
//    /**
//     * Do not expose this method to users because it returns sensitive information about providers.
//     *
//     * @param id
//     * @return
//     */
//    private ProviderBundle getWithCatalogue(String id, String catalogueId) {
//        Resource resource = getResource(id, catalogueId, false);
//        if (resource == null) {
//            throw new CatalogueResourceNotFoundException(String.format(
//                    "Could not find provider with id: %s and catalogueId: %s", id, catalogueId));
//        }
//        return deserialize(resource);
//    }
//
//    public ProviderBundle get(String catalogueId, String providerId, Authentication auth) {
//        ProviderBundle providerBundle = getWithCatalogue(providerId, catalogueId);
//        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
//        if (catalogueBundle == null) {
//            throw new CatalogueResourceNotFoundException(
//                    String.format("Could not find catalogue with id: %s", catalogueId));
//        }
//        if (!providerBundle.getProvider().getCatalogueId().equals(catalogueId)) {
//            throw new ResourceException(String.format("Provider with id [%s] does not belong to the catalogue with id [%s]",
//                    providerId, catalogueId), HttpStatus.CONFLICT);
//        }
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
//    }
//
//    @Override
//    public ProviderBundle get(String id, Authentication auth) {
//        ProviderBundle providerBundle = get(id);
//        if (auth != null && auth.isAuthenticated()) {
//            User user = User.of(auth);
//            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
//            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
//                    securityService.userHasAdminAccess(user, id)) {
//                return providerBundle;
//            }
//        }
//        // else return the Provider ONLY if he is active
//        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
//            return providerBundle;
//        }
//        throw new InsufficientAuthenticationException("You cannot view the specific Provider");
//    }
//
    @Override
    public Paging<ResourceHistory> getHistory(String id, String catalogueId) {
        return null;
//        Map<String, ResourceHistory> historyMap = new TreeMap<>();
//
//        Resource resource = getResource(id, catalogueId, "false");
//        List<Version> versions = versionService.getVersionsByResource(resource.getId());
//        versions.sort((version, t1) -> {
//            if (version.getCreationDate().getTime() < t1.getCreationDate().getTime()) {
//                return -1;
//            }
//            return 1;
//        });
//
//        // create the first entry from the current resource
//        ProviderBundle providerBundle;
//        providerBundle = deserialize(resource);
//        if (providerBundle != null && providerBundle.getMetadata() != null) {
//            historyMap.put(providerBundle.getMetadata().getModifiedAt(), new ResourceHistory(providerBundle, resource.getId()));
//        }
//
//        // create version entries
//        for (Version version : versions) {
//            resource = (version.getResource() == null ? getResource(version.getParentId(), catalogueId, "false") : version.getResource());
//            resource.setPayload(version.getPayload());
//            providerBundle = deserialize(resource);
//            if (providerBundle != null) {
//                try {
//                    historyMap.putIfAbsent(providerBundle.getMetadata().getModifiedAt(), new ResourceHistory(providerBundle, version.getId()));
//                } catch (NullPointerException e) {
//                    logger.warn("Provider with id '{}' does not have Metadata", providerBundle.getId());
//                }
//            }
//        }
//
//        // sort list by modification date
//        List<ResourceHistory> history = new ArrayList<>(historyMap.values());
//        history.sort((resourceHistory, t1) -> {
//            if (Long.parseLong(resourceHistory.getModifiedAt()) < Long.parseLong(t1.getModifiedAt())) {
//                return 1;
//            }
//            return -1;
//        });
//
//        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }
//
//    @Override
//    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
//        List<ProviderBundle> retList = new ArrayList<>();
//
//        // if user is ADMIN or EPOT return everything
//        if (auth != null && auth.isAuthenticated()) {
//            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
//                    securityService.hasRole(auth, "ROLE_EPOT")) {
//                return super.getAll(ff, auth);
//            }
//            // if user is PROVIDER ADMIN return all his Providers (rejected, pending) with their sensitive data (Users, MainContact) too
//            User user = User.of(auth);
//            Browsing<ProviderBundle> providers = super.getAll(ff, auth);
//            for (ProviderBundle providerBundle : providers.getResults()) {
//                if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId()) ||
//                        securityService.userHasAdminAccess(user, providerBundle.getId())) {
//                    retList.add(providerBundle);
//                }
//            }
//            providers.setResults(retList);
//            providers.setTotal(retList.size());
//            providers.setTo(retList.size());
//            return providers;
//        }
//
//        // else return ONLY approved Providers
//        ff.addFilter("status", "approved provider");
//        Browsing<ProviderBundle> providers = super.getAll(ff, auth);
//        retList.addAll(providers.getResults());
//        providers.setResults(retList);
//
//        return providers;
//    }
//
//    @Override
//    public void delete(ProviderBundle provider) {
//        String catalogueId = provider.getProvider().getCatalogueId();
//        // block Public Provider update
//        if (provider.getMetadata().isPublished()) {
//            throw new ValidationException("You cannot directly delete a Public Provider");
//        }
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        logger.trace("User is attempting to delete the Provider with id '{}'", provider.getId());
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
//        logger.debug("Deleting Provider: {} and all his Resources", provider);
//
//        deleteBundle(provider);
//        logger.debug("Deleting Resource {}", provider);
//
//        // TODO: move to aspect
//        registrationMailService.notifyProviderAdminsForProviderDeletion(provider);
//
//        synchronizerService.syncDelete(provider.getProvider());
//
//    }
//
//    private void deleteBundle(ProviderBundle providerBundle) {
//        Resource existingResource = getResource(providerBundle.getId(), providerBundle.getProvider().getCatalogueId(), false);
//        ProviderBundle existingProvider = deserialize(existingResource);
//
//        // block Public Provider update
//        if (existingProvider.getMetadata().isPublished()) {
//            throw new ValidationException("You cannot directly delete a Public Provider");
//        }
//        logger.info("Deleting Provider: {}", existingProvider);
//        existingResource.setPayload(serialize(existingProvider));
//        existingResource.setResourceType(getResourceType());
//        resourceService.deleteResource(existingResource.getId());
//    }
//
    @Override
    public NewProviderBundle verify(String id, String status, Boolean active, Authentication auth) {
        return null;
//        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
//        if (!statusVocabulary.getType().equals("Provider state")) {
//            throw new ValidationException(String.format("Vocabulary %s does not consist a Provider State!", status));
//        }
//        logger.trace("verifyProvider with id: '{}' | status: '{}' | active: '{}'", id, status, active);
//        NewProviderBundle provider = genericResourceService.get(resourceTypeName, id);
//        Resource existingResource = getResource(provider.getId(), provider.getProvider().getCatalogueId(), false);
//        ProviderBundle existingProvider = deserialize(existingResource);
//
//        existingProvider.setStatus(vocabularyService.get(status).getId());
//        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingProvider, auth);
//        LoggingInfo loggingInfo = null;
//
//        switch (status) {
//            case "approved provider":
//                if (active == null) {
//                    active = true;
//                }
//                existingProvider.setActive(active);
//                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
//                        LoggingInfo.ActionType.APPROVED.getKey());
//
//                // add Provider's Name as a HLE Vocabulary
//                checkAndAddProviderToHLEVocabulary(existingProvider);
//                break;
//            case "rejected provider":
//                existingProvider.setActive(false);
//                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
//                        LoggingInfo.ActionType.REJECTED.getKey());
//                break;
//            default:
//                break;
//        }
//        loggingInfoList.add(loggingInfo);
//        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
//        existingProvider.setLoggingInfo(loggingInfoList);
//
//        // latestOnboardingInfo
//        existingProvider.setLatestOnboardingInfo(loggingInfo);
//
//        logger.info("Verifying Provider: {}", existingProvider);
//        existingResource.setPayload(serialize(existingProvider));
//        existingResource.setResourceType(getResourceType());
//        resourceService.updateResource(existingResource);
//        return existingProvider;
    }

    @Override
    public NewProviderBundle publish(String id, Boolean active, Authentication auth) {
        return null;
//        ProviderBundle provider = getWithCatalogue(id, catalogueId);
//        Resource existingResource = getResource(provider.getId(), provider.getProvider().getCatalogueId(), false);
//        ProviderBundle existingProvider = deserialize(existingResource);
//
//        if ((existingProvider.getStatus().equals(vocabularyService.get("pending provider").getId()) ||
//                existingProvider.getStatus().equals(vocabularyService.get("rejected provider").getId())) && !existingProvider.isActive()) {
//            throw new ValidationException(String.format("You cannot activate this Provider, because it's Inactive with status = [%s]", existingProvider.getStatus()));
//        }
//        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingProvider, auth);
//        LoggingInfo loggingInfo;
//
//        if (active == null) {
//            active = false;
//        }
//        existingProvider.setActive(active);
//        if (active) {
//            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
//                    LoggingInfo.ActionType.ACTIVATED.getKey());
//            logger.info("Activating Provider: {}", existingProvider);
//        } else {
//            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
//                    LoggingInfo.ActionType.DEACTIVATED.getKey());
//            logger.info("Deactivating Provider: {}", existingProvider);
//        }
//        activateProviderResources(existingProvider.getId(), active, auth);
//        loggingInfoList.add(loggingInfo);
//        existingProvider.setLoggingInfo(loggingInfoList);
//
//        // latestLoggingInfo
//        existingProvider.setLatestUpdateInfo(loggingInfo);
//        existingProvider.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
//        existingProvider.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));
//
//        existingResource.setPayload(serialize(existingProvider));
//        existingResource.setResourceType(getResourceType());
//        resourceService.updateResource(existingResource);
//        return existingProvider;
    }
//
//    @Override
//    public List<NewProviderBundle> getUserProviders(FacetFilter ff) {
//        return genericResourceService.getResults(ff).getResults()
//                .stream()
//                .map(obj -> (NewProviderBundle) obj)
//                .collect(Collectors.toList());
//    }

    public Browsing<NewProviderBundle> getMy(FacetFilter ff, Authentication auth) {
        if (auth == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }
        if (ff == null) {
            ff = new FacetFilter();
            ff.setQuantity(maxQuantity);
        }
        if (!ff.getFilter().containsKey("published")) {
            ff.addFilter("published", false);
        }
        ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    //
//    @Override
//    public List<ProviderBundle> getInactive() {
//        FacetFilter ff = new FacetFilter();
//        ff.addFilter("active", false);
//        ff.addFilter("published", false);
//        ff.setFrom(0);
//        ff.setQuantity(maxQuantity);
//        ff.addOrderBy("name", "asc");
//        return getAll(ff, null).getResults();
//    }
//
//    public void activateProviderResources(String providerId, Boolean active, Authentication auth) {
//        List<ServiceBundle> services = serviceBundleService.getResourceBundles(providerId, auth);
//        List<TrainingResourceBundle> trainingResources = trainingResourceService.getResourceBundles(providerId, auth);
//        List<DeployableServiceBundle> deployableServices = deployableServiceService.getResourceBundles(providerId, auth);
//        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, providerId, auth).getResults();
//        if (active) {
//            logger.info("Activating all Resources of the Provider with id: '{}'", providerId);
//        } else {
//            logger.info("Deactivating all Resources of the Provider with id: '{}'", providerId);
//        }
//        activateProviderServices(services, active, auth);
//        activateProviderTrainingResources(trainingResources, active, auth);
//        activateProviderDeployableServices(deployableServices, active, auth);
//        activateProviderInteroperabilityRecords(interoperabilityRecords, active, auth);
//    }
//
//    private void activateProviderServices(List<ServiceBundle> services, Boolean active, Authentication auth) {
//        for (ServiceBundle service : services) {
//            if (service.getStatus().equals("approved resource")) {
//                ServiceBundle lowerLevelService = ObjectUtils.clone(service);
//                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(service, active, auth);
//
//                // update Service's fields
//                service.setLoggingInfo(loggingInfoList);
//                service.setLatestUpdateInfo(loggingInfoList.getLast());
//                service.setActive(active);
//
//                try {
//                    logger.debug("Setting Service '{}'-'{}' of the '{}' Catalogue to active: '{}'", service.getId(),
//                            service.getService().getName(), service.getService().getCatalogueId(), service.isActive());
//                    serviceBundleService.update(service, auth);
//                    // TODO: FIX ON ProviderManagementAspect
//                    publicServiceManager.update(service, auth);
//                } catch (ResourceNotFoundException e) {
//                    logger.error("Could not update Service '{}'-'{}' of the '{}' Catalogue", service.getId(),
//                            service.getService().getName(), service.getService().getCatalogueId());
//                }
//
//                // Activate/Deactivate Service's Extensions && Subprofiles
//                serviceBundleService.publishServiceRelatedResources(lowerLevelService.getId(),
//                        lowerLevelService.getService().getCatalogueId(), active, auth);
//            }
//        }
//    }
//
//    private void activateProviderTrainingResources(List<TrainingResourceBundle> trainingResources, Boolean active, Authentication auth) {
//        for (TrainingResourceBundle trainingResourceBundle : trainingResources) {
//            if (trainingResourceBundle.getStatus().equals("approved resource")) {
//                TrainingResourceBundle lowerLevelTrainingResource = ObjectUtils.clone(trainingResourceBundle);
//                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(trainingResourceBundle, active, auth);
//
//                // update Service's fields
//                trainingResourceBundle.setLoggingInfo(loggingInfoList);
//                trainingResourceBundle.setLatestUpdateInfo(loggingInfoList.getLast());
//                trainingResourceBundle.setActive(active);
//
//                try {
//                    logger.debug("Setting Training Resource '{}'-'{}' of the '{}' Catalogue to active: '{}'", trainingResourceBundle.getId(),
//                            trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getCatalogueId(),
//                            trainingResourceBundle.isActive());
//                    trainingResourceService.update(trainingResourceBundle, auth);
//                    // TODO: FIX ON ProviderManagementAspect
//                    publicTrainingResourceManager.update(trainingResourceBundle, auth);
//                } catch (ResourceNotFoundException e) {
//                    logger.error("Could not update Training Resource '{}'-'{}' of the '{}' Catalogue", trainingResourceBundle.getId(),
//                            trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getCatalogueId());
//                }
//
//                // Activate/Deactivate Training Resource's Extensions
//                trainingResourceService.publishTrainingResourceRelatedResources(lowerLevelTrainingResource.getId(),
//                        lowerLevelTrainingResource.getTrainingResource().getCatalogueId(), active, auth);
//            }
//        }
//    }
//
//    private void activateProviderDeployableServices(List<DeployableServiceBundle> deployableServices, Boolean active, Authentication auth) {
//        for (DeployableServiceBundle bundle : deployableServices) {
//            if (bundle.getStatus().equals("approved resource")) {
//                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(bundle, active, auth);
//
//                // update Service's fields
//                bundle.setLoggingInfo(loggingInfoList);
//                bundle.setLatestUpdateInfo(loggingInfoList.getLast());
//                bundle.setActive(active);
//
//                try {
//                    logger.debug("Setting Deployable Service '{}'-'{}' of the '{}' Catalogue to active: '{}'", bundle.getId(),
//                            bundle.getDeployableService().getName(), bundle.getDeployableService().getCatalogueId(),
//                            bundle.isActive());
//                    deployableServiceService.update(bundle, auth);
//                    // TODO: FIX ON ProviderManagementAspect
//                    publicDeployableServiceService.update(bundle, auth);
//                } catch (ResourceNotFoundException e) {
//                    logger.error("Could not update Training Resource '{}'-'{}' of the '{}' Catalogue", bundle.getId(),
//                            bundle.getDeployableService().getName(), bundle.getDeployableService().getCatalogueId());
//                }
//            }
//        }
//    }
//
//    private void activateProviderInteroperabilityRecords(List<InteroperabilityRecordBundle> interoperabilityRecords, Boolean active, Authentication auth) {
//        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecords) {
//            if (interoperabilityRecordBundle.getStatus().equals("approved interoperability record")) {
//                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(interoperabilityRecordBundle, active, auth);
//
//                // update Service's fields
//                interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);
//                interoperabilityRecordBundle.setLatestUpdateInfo(loggingInfoList.getLast());
//                interoperabilityRecordBundle.setActive(active);
//
//                try {
//                    logger.debug("Setting Interoperability Record '{}'-'{}' of the '{}' Catalogue to active: '{}'", interoperabilityRecordBundle.getId(),
//                            interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
//                            interoperabilityRecordBundle.isActive());
//                    interoperabilityRecordService.update(interoperabilityRecordBundle, auth);
//                    // TODO: FIX ON ProviderManagementAspect
//                    publicInteroperabilityRecordManager.update(interoperabilityRecordBundle, auth);
//                } catch (ResourceNotFoundException e) {
//                    logger.error("Could not update Interoperability Record '{}'-'{}' of the '{}' Catalogue", interoperabilityRecordBundle.getId(),
//                            interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
//                }
//            }
//        }
//    }
//
    @Override
    public NewProviderBundle validate(NewProviderBundle provider) {
        logger.debug("Validating Provider with id: '{}'", provider.getProvider().get("id"));
//        Object scientificDomains = provider.getProvider().get("scientificDomains");
//        if (scientificDomains instanceof List<?> scientificDomainsList) {
//            if (!scientificDomainsList.isEmpty()) {
//                validateScientificDomains(scientificDomainsList); //FIXME
//                +
//
//            }
//        }
//        Object merilScientificDomains = provider.getProvider().get("merilScientificDomains");
//        if (merilScientificDomains instanceof List<?> merilScientificDomainsList) {
//            if (!merilScientificDomainsList.isEmpty()) {
//                validateScientificDomains(merilScientificDomainsList); //FIXME
//            }
//        }

        return validate(provider);
    }
//
//
    @Override
    public boolean hasAdminAcceptedTerms(FacetFilter ff, Authentication auth) {
        NewProviderBundle bundle = get(ff, auth);
        List<User> admins = (List<User>) bundle.getProvider().get("users");
        String userEmail = AuthenticationInfo.getEmail(auth).toLowerCase();

        List<String> acceptedTerms = bundle.getMetadata().getTerms();
        if (acceptedTerms == null || acceptedTerms.isEmpty()) {
            return !admins.contains(userEmail); // false -> show modal, true -> no modal
        }
        if (admins.contains(userEmail) && !acceptedTerms.contains(userEmail)) {
            return false; // Show modal
        }
        return true; // No modal
    }

    @Override
    public void adminAcceptedTerms(FacetFilter ff, Authentication auth) {
        NewProviderBundle bundle = get(ff, auth);
        String userEmail = AuthenticationInfo.getEmail(auth);

        List<String> existingTerms = bundle.getMetadata().getTerms();
        if (existingTerms == null) {
            existingTerms = new ArrayList<>();
        }

        if (!existingTerms.contains(userEmail)) {
            existingTerms.add(userEmail);
            bundle.getMetadata().setTerms(existingTerms);

            try {
                genericResourceService.update(resourceTypeName, ff.getFilter().get("resource_internal_id").toString(), bundle);
            } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
                logger.warn("Could not update terms for Provider with id: '{}'", ff.getFilter().get("resource_internal_id"));
            }
        }
    }
//
//    public void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
//        List<String> existingAdmins = new ArrayList<>();
//        List<String> newAdmins = new ArrayList<>();
//        for (User user : existingProvider.getProvider().getUsers()) {
//            existingAdmins.add(user.getEmail().toLowerCase());
//        }
//        for (User user : updatedProvider.getProvider().getUsers()) {
//            newAdmins.add(user.getEmail().toLowerCase());
//        }
//        List<String> adminsAdded = new ArrayList<>(newAdmins);
//        adminsAdded.removeAll(existingAdmins);
//        if (!adminsAdded.isEmpty()) {
//            registrationMailService.sendEmailsToNewlyAddedProviderAdmins(updatedProvider, adminsAdded);
//        }
//        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
//        adminsDeleted.removeAll(newAdmins);
//        if (!adminsDeleted.isEmpty()) {
//            registrationMailService.sendEmailsToNewlyDeletedProviderAdmins(existingProvider, adminsDeleted);
//        }
//    }
//
    @Override
    public void requestProviderDeletion(FacetFilter ff, Authentication auth) {
        NewProviderBundle provider = get(ff, auth);
        for (User user : (List<User>) provider.getProvider().get("users")) {
            if (user.getEmail().equalsIgnoreCase(AuthenticationInfo.getEmail(auth).toLowerCase())) {
//                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth));
            }
        }
    }
//
    @Override
    public NewProviderBundle audit(String providerId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        return null;
//        ProviderBundle provider = get(providerId, catalogueId, false);
//        Resource existingResource = getResource(provider.getId(), provider.getProvider().getCatalogueId(), false);
//        ProviderBundle existingProvider = deserialize(existingResource);
//
//        commonMethods.auditResource(existingProvider, comment, actionType, auth);
//        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
//            existingProvider.setAuditState(Auditable.VALID);
//        }
//        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//            existingProvider.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
//        }
//
//        // send notification emails to Provider Admins
//        registrationMailService.notifyProviderAdminsForBundleAuditing(existingProvider, existingProvider.getProvider().getUsers());
//
//        logger.info("Audited Provider '{}'-'{}' with [actionType: {}]",
//                existingProvider.getProvider().getId(), existingProvider.getProvider().getName(), actionType);
//
//        existingResource.setPayload(serialize(existingProvider));
//        existingResource.setResourceType(getResourceType());
//        resourceService.updateResource(existingResource);
//        return existingProvider;
    }


    @Override
    public Paging<NewProviderBundle> getRandomResources(FacetFilter ff, String auditingInterval) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved provider");
        facetFilter.addFilter("published", false);

        Browsing<NewProviderBundle> providersBrowsing = genericResourceService.getResults(facetFilter);
        List<NewProviderBundle> providersToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(Integer.parseInt(auditingInterval))
                .toEpochSecond();

        for (NewProviderBundle providerBundle : providersBrowsing.getResults()) {
            LoggingInfo auditInfo = providerBundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include providers that have never been audited
                providersToBeAudited.add(providerBundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include providers that were last audited before the threshold
                        providersToBeAudited.add(providerBundle);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(providersToBeAudited);

        // Limit the list to the requested quantity
        int quantity = ff.getQuantity();
        if (providersToBeAudited.size() > quantity) {
            providersToBeAudited = providersToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(providersToBeAudited.size(), 0, providersToBeAudited.size(), providersToBeAudited,
                providersBrowsing.getFacets());
    }
//
//    private ProviderBundle onboard(ProviderBundle provider, String catalogueId, Authentication auth) {
//        // create LoggingInfo
//        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);
//        provider.setLoggingInfo(loggingInfoList);
//        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
//            // set catalogueId = eosc
//            provider.getProvider().setCatalogueId(this.catalogueId);
//            provider.setActive(false);
//            provider.setStatus(vocabularyService.get("pending provider").getId());
//            provider.setTemplateStatus(vocabularyService.get("no template status").getId());
//            provider.setId(idCreator.generate(getResourceTypeName()));
//            commonMethods.createIdentifiers(provider, getResourceTypeName(), false);
//        } else {
//            commonMethods.checkCatalogueIdConsistency(provider, catalogueId);
//            provider.setActive(true);
//            provider.setStatus(vocabularyService.get("approved provider").getId());
//            provider.setTemplateStatus(vocabularyService.get("approved template").getId());
//            loggingInfoList.add(commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
//                    LoggingInfo.ActionType.APPROVED.getKey()));
//            // check that external source has provided its own ID
//            idCreator.validateId(provider.getId());
//            commonMethods.createIdentifiers(provider, getResourceTypeName(), true);
//        }
//        provider.setAuditState(Auditable.NOT_AUDITED);
//        provider.setLatestOnboardingInfo(loggingInfoList.getLast());
//
//        return provider;
//    }
//
//    private void checkAndAddProviderToHLEVocabulary(ProviderBundle providerBundle) {
//        if (providerBundle.getStatus().equals("approved provider") && providerBundle.getProvider().isLegalEntity()) {
//            List<String> allHLENames = vocabularyService.getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY)
//                    .stream().map(Vocabulary::getName).toList();
//            if (!allHLENames.contains(providerBundle.getProvider().getName())) {
//                addApprovedProviderToHLEVocabulary(providerBundle);
//            }
//        }
//    }
//
//    private void addApprovedProviderToHLEVocabulary(ProviderBundle providerBundle) {
//        Vocabulary hle = new Vocabulary();
//        hle.setId("provider_hosting_legal_entity-" + idCreator.sanitizeString(providerBundle.getProvider().getName()));
//        hle.setName(providerBundle.getProvider().getName());
//        hle.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
//        hle.setExtras(new HashMap<>() {{
//            put("catalogueId", providerBundle.getProvider().getCatalogueId());
//        }});
//        logger.info("Creating a new Hosting Legal Entity Vocabulary with id: [{}] and name: [{}]",
//                hle.getId(), hle.getName());
//        vocabularyService.add(hle, null);
//    }
//
//    @Override
//    public Paging<?> getRejectedResources(FacetFilter ff, String resourceType, Authentication auth) {
//        if (resourceType.equals("service")) {
//            Browsing<ServiceBundle> providerRejectedResources = getResourceBundles(ff, serviceBundleService, auth);
//            return new Paging<>(providerRejectedResources);
//        } else if (resourceType.equals("training_resource")) {
//            Browsing<TrainingResourceBundle> providerRejectedResources = getResourceBundles(ff, trainingResourceService, auth);
//            return new Paging<>(providerRejectedResources);
//        }
//        return null;
//    }
//
//    private <T extends Bundle<?>, I extends ResourceCRUDService<T, Authentication>> Browsing<T> getResourceBundles(FacetFilter ff, I service, Authentication auth) {
//        FacetFilter filter = new FacetFilter();
//        filter.setFrom(ff.getFrom());
//        filter.setQuantity(ff.getQuantity());
//        filter.setKeyword(ff.getKeyword());
//        filter.setFilter(ff.getFilter());
//        filter.setOrderBy(ff.getOrderBy());
//        // Get all Catalogue's Resources
//        return service.getAll(filter, auth);
//    }
//
    @Override
    public NewProviderBundle createPublicProvider(NewProviderBundle providerBundle, Authentication auth) {
//        publicProviderService.add(providerBundle, auth);
//        return providerBundle;
        return null;
    }
//
//
    @Override
    public NewProviderBundle suspend(String providerId, String catalogueId, boolean suspend, Authentication auth) {
        return null;
//        ProviderBundle providerBundle = get(providerId, catalogueId, false);
//        Resource existingResource = getResource(providerBundle.getId(), providerBundle.getProvider().getCatalogueId(), false);
//        ProviderBundle existingProvider = deserialize(existingResource);
//        commonMethods.suspensionValidation(existingProvider, catalogueId, providerId, suspend, auth);
//
//        // Suspend Provider
//        commonMethods.suspendResource(existingProvider, suspend, auth);
//        existingResource.setPayload(serialize(existingProvider));
//        existingResource.setResourceType(getResourceType());
//        resourceService.updateResource(existingResource);
//
//        // Suspend Provider's resources
//        List<ServiceBundle> services = serviceBundleService.getResourceBundles(catalogueId, providerId, auth).getResults();
//        List<TrainingResourceBundle> trainingResources = trainingResourceService.getResourceBundles(catalogueId, providerId, auth).getResults();
//        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, providerId, auth).getResults();
//
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
//
//        return providerBundle;
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
        List<NewProviderBundle> providers = genericResourceService.getResults(ff).getResults()
                .stream()
                .map(obj -> (NewProviderBundle) obj)
                .toList();;
        List<ServiceBundle> services = new ArrayList<>();
        List<TrainingResourceBundle> trainingResources = new ArrayList<>();
        List<InteroperabilityRecordBundle> interoperabilityRecords = new ArrayList<>();
        createMapValuesForHLE(providers, "provider", mapValuesList);
        for (NewProviderBundle bundle : providers) {
//            services.addAll(serviceBundleService.getResourceBundles(bundle.getProvider().getCatalogueId(),
//                    bundle.getId(), auth).getResults());
//            trainingResources.addAll(trainingResourceService.getResourceBundles(bundle.getProvider().
//                    getCatalogueId(), bundle.getId(), auth).getResults());
//            interoperabilityRecords.addAll(interoperabilityRecordService.getInteroperabilityRecordBundles(bundle.
//                    getProvider().getCatalogueId(), bundle.getId(), auth).getResults());
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
                    ProviderBundle providerBundle = (ProviderBundle) obj;
                    value.setId(providerBundle.getId());
                    value.setName(providerBundle.getProvider().getName());
                    value.setCatalogue(providerBundle.getProvider().getCatalogueId());
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

    public NewProviderBundle get(FacetFilter ff, Authentication auth) {
        List<NewProviderBundle> providers = genericResourceService.getResults(ff).getResults()
                .stream()
                .map(obj -> (NewProviderBundle) obj)
                .toList();
        if (providers.isEmpty()) {
            throw new CatalogueResourceNotFoundException(
                    String.format("Could not find provider with id: %s and catalogueId: %s",
                            ff.getFilter().get("resource_internal_id"), ff.getFilter().get("catalogue_id")));
        }
        if (canView(providers.getFirst(), auth)) {
            return providers.getFirst();
        }
        throw new InsufficientAuthenticationException("You cannot view the specific Provider");
    }

    private boolean canView(NewProviderBundle bundle, Authentication auth) {
        if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                securityService.hasRole(auth, "ROLE_EPOT") ||
                bundle.getStatus().equals(vocabularyService.get("approved provider").getId()))
            return true;
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            return securityService.userHasAdminAccess(user, bundle.getProvider().get("id").toString());
        }
        return false;
    }

//    private Resource getResource(String id, String catalogueId, String published) {
//        Map<String, String> fields = new HashMap<>();
//        fields.put("resource_internal_id", id);
//        if (catalogueId != null) {
//            fields.put("catalogue_id", catalogueId);
//        }
//        fields.put("published", published);
//        return genericResourceService.get(resourceTypeName, fields, false);
//    }
}
