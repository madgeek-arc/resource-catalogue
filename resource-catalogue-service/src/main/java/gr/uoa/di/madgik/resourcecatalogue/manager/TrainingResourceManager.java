/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.*;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;
import static java.util.stream.Collectors.toList;

@org.springframework.stereotype.Service
public class TrainingResourceManager extends ResourceCatalogueManager<TrainingResourceBundle> implements TrainingResourceService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingResourceManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService catalogueService;
    private final PublicTrainingResourceService publicTrainingResourceManager;
    private final MigrationService migrationService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final RelationshipValidator relationshipValidator;
    @Autowired
    private FacetLabelService facetLabelService;
    @Autowired
    private FieldValidator fieldValidator;
    @Autowired
    private SearchService searchService;
    @Autowired
    @Qualifier("trainingResourceSync")
    private final SynchronizerService<TrainingResource> synchronizerService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public TrainingResourceManager(ProviderService providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   CatalogueService catalogueService,
                                   PublicTrainingResourceService publicTrainingResourceManager,
                                   SynchronizerService<TrainingResource> synchronizerService,
                                   @Lazy ProviderResourcesCommonMethods commonMethods,
                                   @Lazy MigrationService migrationService,
                                   @Lazy RelationshipValidator relationshipValidator) {
        super(TrainingResourceBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.synchronizerService = synchronizerService;
        this.commonMethods = commonMethods;
        this.migrationService = migrationService;
        this.relationshipValidator = relationshipValidator;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        return add(trainingResourceBundle, null, auth);
    }

    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) { // add catalogue provider
            trainingResourceBundle.getTrainingResource().setCatalogueId(this.catalogueId);
            trainingResourceBundle.setId(idCreator.generate(getResourceTypeName()));
            commonMethods.createIdentifiers(trainingResourceBundle, getResourceTypeName(), false);
        } else { // add provider from external catalogue
            commonMethods.checkCatalogueIdConsistency(trainingResourceBundle, catalogueId);
            idCreator.validateId(trainingResourceBundle.getId());
            commonMethods.createIdentifiers(trainingResourceBundle, getResourceTypeName(), true);
        }
        relationshipValidator.checkRelatedResourceIDsConsistency(trainingResourceBundle);

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new CatalogueResourceNotFoundException(String.format("Provider with id '%s' and catalogueId '%s' does not exist",
                    trainingResourceBundle.getTrainingResource().getResourceOrganisation(), trainingResourceBundle.getTrainingResource().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    trainingResourceBundle.getTrainingResource().getResourceOrganisation()), HttpStatus.CONFLICT);
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource Template.",
                    providerBundle.getId()), HttpStatus.CONFLICT);
        }
        validateTrainingResource(trainingResourceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        trainingResourceBundle.setActive(active);

        // create new Metadata if not exists
        if (trainingResourceBundle.getMetadata() == null) {
            trainingResourceBundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth)));
        }

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(trainingResourceBundle, auth);

        // latestOnboardingInfo
        trainingResourceBundle.setLatestOnboardingInfo(loggingInfoList.getFirst());

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            trainingResourceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            trainingResourceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            trainingResourceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        trainingResourceBundle.setLoggingInfo(loggingInfoList);
        trainingResourceBundle.setAuditState(Auditable.NOT_AUDITED);

        logger.info("Adding Training Resource: {}", trainingResourceBundle);
        TrainingResourceBundle ret;

        ret = super.add(trainingResourceBundle, auth);

        synchronizerService.syncAdd(ret.getTrainingResource());

        return ret;
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, String comment, Authentication auth) {
        return update(trainingResourceBundle, trainingResourceBundle.getTrainingResource().getCatalogueId(), comment, auth);
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, String catalogueId, String comment, Authentication auth) {

        TrainingResourceBundle ret = ObjectUtils.clone(trainingResourceBundle);
        TrainingResourceBundle existingTrainingResource;
        existingTrainingResource = get(ret.getTrainingResource().getId(), ret.getTrainingResource().getCatalogueId(), false);
        if (ret.getTrainingResource().equals(existingTrainingResource.getTrainingResource())) {
            return ret;
        }

        if (catalogueId == null || catalogueId.isEmpty()) {
            ret.getTrainingResource().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }
        relationshipValidator.checkRelatedResourceIDsConsistency(ret);

        logger.trace("Attempting to update the Training Resource with id '{}' of the Catalogue '{}'",
                ret.getTrainingResource().getId(), ret.getTrainingResource().getCatalogueId());
        validateTrainingResource(ret);

        ProviderBundle providerBundle = providerService.get(ret.getTrainingResource().getCatalogueId(),
                ret.getTrainingResource().getResourceOrganisation(), auth);

        // block Public Training Resource update
        if (existingTrainingResource.getMetadata().isPublished()) {
            throw new ResourceException("You cannot directly update a Public Training Resource", HttpStatus.FORBIDDEN);
        }

        // update existing TrainingResource Metadata, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingTrainingResource.getMetadata(), AuthenticationInfo.getFullName(auth)));
        ret.setIdentifiers(existingTrainingResource.getIdentifiers());
//        ret.setMigrationStatus(existingTrainingResource.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingTrainingResource, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // set active/status
        ret.setActive(existingTrainingResource.isActive());
        ret.setStatus(existingTrainingResource.getStatus());
        ret.setSuspended(existingTrainingResource.isSuspended());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingTrainingResource.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                ret.setStatus(vocabularyService.get("pending resource").getId());
                ret.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingTrainingResource.getTrainingResource().getCatalogueId().equals(ret.getTrainingResource().getCatalogueId())) {
                throw new ResourceException("You cannot change catalogueId", HttpStatus.FORBIDDEN);
            }
        }

        logger.info("Updating Training Resource: {}", ret);
        ret = super.update(ret, auth);

        synchronizerService.syncUpdate(ret.getTrainingResource());

        // send notification emails to Portal Admins
        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidTrainingResourceUpdate(ret);
            }
        }

        return ret;
    }

    @Override
    public Browsing<TrainingResourceBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        filter.addFilter("resource_organisation", providers.stream().map(ProviderBundle::getId).toList());
        filter.setResourceType(getResourceTypeName());
        return this.getAll(filter, auth);
    }

    @Override
    public TrainingResourceBundle getCatalogueResource(String catalogueId, String trainingResourceId, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(trainingResourceId, catalogueId, false);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(catalogueId, "Catalogue");
        }
        if (!trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
            throw new CatalogueResourceNotFoundException(String.format("Training Resource with id [%s] does not belong to the" +
                    " catalogue with id [%s]", trainingResourceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceAdmin(user, trainingResourceId)) {
                return trainingResourceBundle;
            }
        }
        // else return the Training Resource ONLY if it is active
        if (trainingResourceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return trainingResourceBundle;
        }
        throw new InsufficientAuthenticationException("You cannot view the specific Training Resource");
    }

    @Override
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        String catalogueId = trainingResourceBundle.getTrainingResource().getCatalogueId();
        commonMethods.blockResourceDeletion(trainingResourceBundle.getStatus(), trainingResourceBundle.getMetadata().isPublished());
        commonMethods.deleteResourceInteroperabilityRecords(trainingResourceBundle.getId(), "TrainingResource");
        logger.info("Deleting Training Resource: {}", trainingResourceBundle);
        super.delete(trainingResourceBundle);
        synchronizerService.syncDelete(trainingResourceBundle.getTrainingResource());
    }

    public TrainingResourceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status: '{}' | active: '{}'", id, status, active);
        TrainingResourceBundle trainingResourceBundle = getCatalogueResource(catalogueId, id, auth);
        trainingResourceBundle.markOnboard(vocabularyService.get(status).getId(), active, auth, null);
        ProviderBundle resourceProvider = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);

        switch (status) {
            case "pending resource" -> resourceProvider.setTemplateStatus("pending template");
            case "approved resource" -> resourceProvider.setTemplateStatus("approved template");
            case "rejected resource" -> resourceProvider.setTemplateStatus("rejected template");
        }

        logger.info("Verifying Training Resource: {}", trainingResourceBundle);
        providerService.update(resourceProvider, auth);
        return update(trainingResourceBundle, auth);
    }

    public boolean validateTrainingResource(TrainingResourceBundle trainingResourceBundle) {
        TrainingResource trainingResource = trainingResourceBundle.getTrainingResource();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Training Resource with id: {}", trainingResource.getId());

        try {
            fieldValidator.validate(trainingResourceBundle);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
        if (trainingResourceBundle.getTrainingResource().getScientificDomains() != null &&
                !trainingResourceBundle.getTrainingResource().getScientificDomains().isEmpty()) {
            validateScientificDomains(trainingResourceBundle.getTrainingResource().getScientificDomains());
        }

        return true;
    }

    @Override
    public TrainingResourceBundle publish(String trainingResourceId, Boolean active, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle;
        String activeProvider = "";
        trainingResourceBundle = get(trainingResourceId, catalogueId, false);

        if ((trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                trainingResourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !trainingResourceBundle.isActive()) {
            throw new ResourceException(String.format("You cannot activate this Training Resource, because it's Inactive with status = [%s]",
                    trainingResourceBundle.getStatus()), HttpStatus.CONFLICT);
        }

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
        }
        if (active && activeProvider.isEmpty()) {
            throw new ResourceException("Training Resource does not have active Providers", HttpStatus.CONFLICT);
        }
        trainingResourceBundle.markActive(active, auth);

        update(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    @Override
    public TrainingResourceBundle audit(String trainingResourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        TrainingResourceBundle trainingResource = get(trainingResourceId, catalogueId, false);
        trainingResource.markAudit(comment, actionType, auth);

        // send notification emails to Provider Admins
        ProviderBundle provider = providerService.get(trainingResource.getTrainingResource().getCatalogueId(),
                trainingResource.getTrainingResource().getResourceOrganisation(), auth);
        registrationMailService.notifyProviderAdminsForBundleAuditing(trainingResource, provider.getProvider().getUsers());

        logger.info("Audited Training Resource '{}'-'{}' with [actionType: {}]",
                trainingResource.getTrainingResource().getId(), trainingResource.getTrainingResource().getTitle(), actionType);
        return super.update(trainingResource, auth);
    }

    @Override
    public List<TrainingResourceBundle> getResourceBundles(String providerId, Authentication auth) {
        return getResourceBundles(catalogueId, providerId, auth).getResults();
    }

    @Override
    public Paging<TrainingResourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<TrainingResourceBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, null).getResults();
    }

    @Override
    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(resourceId, catalogueId, false);
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
                trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
        logger.info("Mailing provider '{}'-'{}' for outdated Training Resources", providerBundle.getId(), providerBundle.getProvider().getName());
        registrationMailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(trainingResourceBundle, providerBundle);
    }

    @Override
    public TrainingResourceBundle createPublicResource(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        publicTrainingResourceManager.add(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<TrainingResource> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
    }

    @Override
    public TrainingResourceBundle getOrElseReturnNull(String id) {
        TrainingResourceBundle trainingResourceBundle;
        try {
            trainingResourceBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return trainingResourceBundle;
    }

    @Override
    public Browsing<TrainingResourceBundle> getAll(FacetFilter ff, Authentication auth) {
        updateFacetFilterConsideringTheAuthorization(ff, auth);

        ff.setResourceType(getResourceTypeName());
        Browsing<TrainingResourceBundle> resources;
        resources = getResults(ff);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.generateLabels(resources.getFacets()));
        }
        return resources;
    }

    private void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth) {
        // if user is Unauthorized, return active ONLY
        if (auth == null || !auth.isAuthenticated() || (
                !securityService.hasRole(auth, "ROLE_PROVIDER") &&
                        !securityService.hasRole(auth, "ROLE_EPOT") &&
                        !securityService.hasRole(auth, "ROLE_ADMIN"))) {
            filter.addFilter("active", true);
            filter.addFilter("published", false);
        }
    }

    @Override
    public Paging<TrainingResourceBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved resource");
        facetFilter.addFilter("published", false);

        Browsing<TrainingResourceBundle> trainingsBrowsing = getAll(facetFilter, auth);
        List<TrainingResourceBundle> trainingsToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(auditingInterval)
                .toEpochSecond();

        for (TrainingResourceBundle trainingResourceBundle : trainingsBrowsing.getResults()) {
            LoggingInfo auditInfo = trainingResourceBundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include training resources that have never been audited
                trainingsToBeAudited.add(trainingResourceBundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include training resources that were last audited before the threshold
                        trainingsToBeAudited.add(trainingResourceBundle);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(trainingsToBeAudited);

        // Limit the list to the requested quantity
        if (trainingsToBeAudited.size() > quantity) {
            trainingsToBeAudited = trainingsToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(trainingsToBeAudited.size(), 0, trainingsToBeAudited.size(), trainingsToBeAudited,
                trainingsBrowsing.getFacets());
    }

    @Override
    public TrainingResourceBundle getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<TrainingResourceBundle> allProviderTrainingResources = getAll(ff, auth).getResults();
        for (TrainingResourceBundle trainingResourceBundle : allProviderTrainingResources) {
            if (trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return trainingResourceBundle;
            }
        }
        return null;
    }

    @Override
    public List<TrainingResource> getByIds(Authentication auth, String... ids) {
        List<TrainingResource> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId, false).getTrainingResource();
                    } catch (CatalogueResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
    }

    @Override
    public TrainingResourceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(resourceId, catalogueId, false);
        // check Datasource's status
        if (!trainingResourceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Training Resource with id [%s] to another Provider as it" +
                    "is not yet Approved", trainingResourceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot move a Training Resource to a Provider of another Catalogue");
        }

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = trainingResourceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment.isEmpty()) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        trainingResourceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = trainingResourceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(AuthenticationInfo.getFullName(auth));
        metadata.setTerms(null);
        trainingResourceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(newProviderId);

        // update ResourceProviders
        List<String> resourceProviders = trainingResourceBundle.getTrainingResource().getResourceProviders();
        if (resourceProviders.contains(oldProvider.getId())) {
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // add Resource, delete the old one
        add(trainingResourceBundle, auth);
        publicTrainingResourceManager.delete(get(resourceId, catalogueId, false)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueId, false));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, trainingResourceBundle.getId());

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, trainingResourceBundle, auth);

        return trainingResourceBundle;
    }

    @Override
    public TrainingResourceBundle suspend(String trainingResourceId, String catalogueId, boolean suspend, Authentication auth) {
        TrainingResourceBundle existingTR = get(trainingResourceId, catalogueId, false);
        commonMethods.suspensionValidation(existingTR, existingTR.getTrainingResource().getCatalogueId(),
                existingTR.getTrainingResource().getResourceOrganisation(), suspend, auth);
        existingTR.markSuspend(suspend, auth);
        return super.update(existingTR, auth);
    }
}
