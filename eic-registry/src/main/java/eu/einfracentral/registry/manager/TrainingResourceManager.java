package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.SortUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;

@org.springframework.stereotype.Service
public class TrainingResourceManager extends ResourceManager<TrainingResourceBundle> implements TrainingResourceService<TrainingResourceBundle> {

    private static final Logger logger = LogManager.getLogger(ServiceBundleManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    public TrainingResourceManager(ProviderService<ProviderBundle, Authentication> providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(TrainingResourceBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
    }

    @Override
    public String getResourceType() {
        return "training_resource";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle addResource(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        return addResource(trainingResourceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle addResource(TrainingResourceBundle trainingResourceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.equals("")) { // add catalogue provider
            trainingResourceBundle.getTrainingResource().setCatalogueId(catalogueName);
        } else { // add provider from external catalogue
            checkCatalogueIdConsistency(trainingResourceBundle, catalogueId);
        }

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' and catalogueId '%s' does not exist", trainingResourceBundle.getTrainingResource().getResourceOrganisation(), trainingResourceBundle.getTrainingResource().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    trainingResourceBundle.getTrainingResource().getResourceOrganisation()));
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ValidationException(String.format("The Provider with id %s has already registered a Resource Template.", providerBundle.getId()));
        }
        try {
            trainingResourceBundle.setId(idCreator.createTrainingResourceId(trainingResourceBundle));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        validate(trainingResourceBundle);
        if (trainingResourceBundle.getTrainingResource().getScientificDomains() != null &&
                !trainingResourceBundle.getTrainingResource().getScientificDomains().isEmpty()){
            validateScientificDomains(trainingResourceBundle.getTrainingResource().getScientificDomains());
        }

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        trainingResourceBundle.setActive(active);

        // create new Metadata if not exists
        if (trainingResourceBundle.getMetadata() == null) {
            trainingResourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        trainingResourceBundle.setLatestOnboardingInfo(loggingInfo);

        sortFields(trainingResourceBundle);

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            trainingResourceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            trainingResourceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            trainingResourceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        logger.info("Adding Training Resource: {}", trainingResourceBundle);
        TrainingResourceBundle ret;
        ret = add(trainingResourceBundle, auth);

        return ret;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Training Resource: {}", auth, trainingResourceBundle);
        if (trainingResourceBundle.getTrainingResource().getId() == null) {
            try {
                trainingResourceBundle.getTrainingResource().setId(idCreator.createTrainingResourceId(trainingResourceBundle));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        if (exists(trainingResourceBundle)) {
            throw new ResourceException("Training Resource already exists!", HttpStatus.CONFLICT);
        }

        String serialized;
        serialized = parserPool.serialize(trainingResourceBundle, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);

        resourceService.addResource(created);

        return trainingResourceBundle;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle updateResource(TrainingResourceBundle trainingResourceBundle, String comment, Authentication auth) {
        return updateResource(trainingResourceBundle, trainingResourceBundle.getTrainingResource().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle updateResource(TrainingResourceBundle trainingResourceBundle, String catalogueId, String comment, Authentication auth) {

        TrainingResourceBundle ret;
        TrainingResourceBundle existingTrainingResourceBundle;
        try { // try to find a TrainingResourceBundle with the same id
            existingTrainingResourceBundle = get(trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Training Resource with id [%s] on the [%s] Catalogue",
                    trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId()));
        }

        // check if there are actual changes in the Training Resource
        if (trainingResourceBundle.getTrainingResource().equals(existingTrainingResourceBundle.getTrainingResource())){
            throw new ValidationException("There are no changes in the Training Resource", HttpStatus.OK);
        }

        if (catalogueId == null || catalogueId.equals("")) {
            trainingResourceBundle.getTrainingResource().setCatalogueId(catalogueName);
        } else {
            checkCatalogueIdConsistency(trainingResourceBundle, catalogueId);
        }

        logger.trace("User '{}' is attempting to update the Training Resource with id '{}' of the Catalogue '{}'", auth, trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        validate(trainingResourceBundle);
        if (trainingResourceBundle.getTrainingResource().getScientificDomains() != null &&
                !trainingResourceBundle.getTrainingResource().getScientificDomains().isEmpty()){
            validateScientificDomains(trainingResourceBundle.getTrainingResource().getScientificDomains());
        }

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);

        // block Public Training Resource update
        if (existingTrainingResourceBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Training Resource");
        }

        User user = User.of(auth);

        // update existing TrainingResource Metadata, Identifiers, MigrationStatus
        trainingResourceBundle.setMetadata(Metadata.updateMetadata(trainingResourceBundle.getMetadata(), user.getFullName()));
        trainingResourceBundle.setMigrationStatus(trainingResourceBundle.getMigrationStatus());

        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        if (existingTrainingResourceBundle.getLoggingInfo() != null) {
            loggingInfoList = existingTrainingResourceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        } else {
            loggingInfoList.add(loggingInfo);
        }
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        trainingResourceBundle.setLatestUpdateInfo(loggingInfo);
        trainingResourceBundle.setActive(existingTrainingResourceBundle.isActive());
        sortFields(trainingResourceBundle);

        // set status
        trainingResourceBundle.setStatus(existingTrainingResourceBundle.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingTrainingResourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                trainingResourceBundle.setStatus(vocabularyService.get("pending resource").getId());
                trainingResourceBundle.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingTrainingResourceBundle.getTrainingResource().getCatalogueId().equals(trainingResourceBundle.getTrainingResource().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        ret = update(trainingResourceBundle, auth);
        logger.info("Updating Training Resource: {}", trainingResourceBundle);

        // send notification emails to Portal Admins
        if (trainingResourceBundle.getLatestAuditInfo() != null && trainingResourceBundle.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(trainingResourceBundle.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(trainingResourceBundle.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && trainingResourceBundle.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                // FIXME: registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(trainingResourceBundle, trainingResourceBundle.getTrainingResource().getTitle());
            }
        }

        return ret;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Training Resource: {}", auth, trainingResourceBundle);

        Resource existing = getResource(trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Training Resource with id '%s' because it does not exist",
                            trainingResourceBundle.getTrainingResource().getId()));
        }

        existing.setPayload(serialize(trainingResourceBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);

        return trainingResourceBundle;
    }

    public TrainingResourceBundle getCatalogueResource(String catalogueId, String trainingResourceId, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(trainingResourceId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (trainingResourceBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Training Resource with id: %s", trainingResourceId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
            throw new ValidationException(String.format("Training Resource with id [%s] does not belong to the catalogue with id [%s]", trainingResourceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceProviderAdmin(user, trainingResourceId, catalogueId)) {
                return trainingResourceBundle;
            }
        }
        // else return the Training Resource ONLY if it is active
        if (trainingResourceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return trainingResourceBundle;
        }
        throw new ValidationException("You cannot view the specific Training Resource");
    }

    @Override
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        // block Public Training Resource deletion
        if (trainingResourceBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Training Resource");
        }
        logger.info("Deleting Training Resource: {}", trainingResourceBundle);
        super.delete(trainingResourceBundle);
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        String[] parts = id.split("\\.");
        String providerId = parts[0];
        TrainingResourceBundle trainingResourceBundle = null;
        List<TrainingResourceBundle> trainingResourceBundles = getResourceBundles(providerId, auth);
        for (TrainingResourceBundle trainingResource : trainingResourceBundles) {
            if (trainingResource.getTrainingResource().getId().equals(id)) {
                trainingResourceBundle = trainingResource;
            }
        }
        if (trainingResourceBundle == null) {
            throw new ValidationException(String.format("The Training Resource with id '%s' does not exist", id));
        }
        trainingResourceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        User user = User.of(auth);

        if (trainingResourceBundle.getLoggingInfo() != null) {
            loggingInfoList = trainingResourceBundle.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                trainingResourceBundle.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                trainingResourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                trainingResourceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                trainingResourceBundle.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                trainingResourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                trainingResourceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        logger.info("Verifying Training Resource: {}", trainingResourceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return update(trainingResourceBundle, auth);
    }

    @Override
    public Paging<TrainingResourceBundle> getInactiveResources() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        return getAll(ff, null);
    }

    @Override
    public TrainingResourceBundle publish(String trainingResourceId, Boolean active, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle;
        String activeProvider = "";
        trainingResourceBundle = this.get(trainingResourceId, catalogueName);

        if ((trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                trainingResourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !trainingResourceBundle.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Training Resource, because it's Inactive with status = [%s]", trainingResourceBundle.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Training Resource does not have active Providers", HttpStatus.CONFLICT);
        }
        trainingResourceBundle.setActive(active);

        User user = User.of(auth);
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        if (active) {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.ACTIVATED.getKey());
        } else {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DEACTIVATED.getKey());
        }
        if (trainingResourceBundle.getLoggingInfo() != null) {
            loggingInfoList = trainingResourceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
            loggingInfoList.add(loggingInfo);
        }
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        trainingResourceBundle.setLatestUpdateInfo(loggingInfo);

        update(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    public TrainingResourceBundle auditResource(String trainingResourceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(trainingResourceId, catalogueName);
        User user = User.of(auth);
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (trainingResourceBundle.getLoggingInfo() != null) {
            loggingInfoList = trainingResourceBundle.getLoggingInfo();
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(), actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        trainingResourceBundle.setLatestAuditInfo(loggingInfo);

        // send notification emails to Provider Admins
        // FIXME: registrationMailService.notifyProviderAdminsForResourceAuditing(trainingResourceBundle);

        logger.info("Auditing Resource: {}", trainingResourceBundle);
        return update(trainingResourceBundle, auth);
    }

    @Override
    public List<TrainingResourceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<TrainingResourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth);
    }

    @Override
    public List<TrainingResource> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Training Resources
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle)) {
                return this.getAll(ff, auth).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
            }
        }
        // else return Provider's Training Resources ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
            return this.getAll(ff, null).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Training Resources of the specific Provider");
    }

    @Override
    public List<TrainingResource> getActiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("active", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
    }

    @Override
    public List<TrainingResourceBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults();
    }

    //    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle = new TrainingResourceBundle();
        try {
            trainingResourceBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources) {
                TrainingResourceBundle trainingResource = deserialize(resource);
                if (trainingResource.getLoggingInfo() != null) {
                    loggingInfoList.addAll(trainingResource.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info(String.format("Training Resource with id [%s] not found", id));
        }
        return null;
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        String providerId = providerService.get(get(resourceId).getTrainingResource().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getTrainingResource().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Training Resources", providerId, providerName));
        //FIXME: registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public TrainingResourceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(resourceId, catalogueName);
        // check Training Resource's status
        if (!trainingResourceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Training Resource with id [%s] to another Provider as it" +
                    "is not yet Approved", trainingResourceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(catalogueName, newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(catalogueName, trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = trainingResourceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || "".equals(comment)) {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        trainingResourceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = trainingResourceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        trainingResourceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(newProviderId);

        // update id
        String initialId = trainingResourceBundle.getId();
        String[] parts = initialId.split("\\.");
        String serviceId = parts[1];
        String newResourceId = newProviderId + "." + serviceId;
        trainingResourceBundle.setId(newResourceId);
        trainingResourceBundle.getTrainingResource().setId(newResourceId);

        // add Resource, delete the old one
        add(trainingResourceBundle, auth);
        delete(get(resourceId, catalogueName));
        //FIXME: publicTrainingResourceManager.delete(get(resourceId, catalogueName)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered

        // emails to EPOT, old and new Provider
        //FIXME: registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, trainingResourceBundle, auth);

        return trainingResourceBundle;
    }

    public TrainingResourceBundle createPublicResource(TrainingResourceBundle trainingResourceBundle, Authentication auth){
        //FIXME: publicTrainingResourceManager.add(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    public void checkCatalogueIdConsistency(TrainingResourceBundle trainingResourceBundle, String catalogueId) {
        catalogueService.existsOrElseThrow(catalogueId);
        if (trainingResourceBundle != null) {
            if (trainingResourceBundle.getTrainingResource().getCatalogueId() == null || trainingResourceBundle.getTrainingResource().getCatalogueId().equals("")) {
                throw new ValidationException("Training Resource's 'catalogueId' cannot be null or empty");
            } else {
                if (!trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
                    throw new ValidationException("Parameter 'catalogueId' and Training Resource's 'catalogueId' don't match");
                }
            }
        }
    }

    public void sortFields(TrainingResourceBundle trainingResourceBundle) {
        trainingResourceBundle.getTrainingResource().setGeographicalAvailabilities(SortUtils.sort(trainingResourceBundle.getTrainingResource().getGeographicalAvailabilities()));
    }

    public TrainingResourceBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Training Resource with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    public Resource getResource(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("%s_id = \"%s\"  AND catalogue_id = \"%s\"", resourceType.getName(), id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources.getTotal() > 0) {
            return resources.getResults().get(0);
        }
        return null;
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<TrainingResource> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
    }

    public List<Resource> getResources(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("%s_id = \"%s\"  AND catalogue_id = \"%s\"", resourceType.getName(), id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources != null) {
            return resources.getResults();
        }
        return Collections.emptyList();
    }

    public TrainingResourceBundle getOrElseReturnNull(String id, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle;
        try {
            trainingResourceBundle = get(id, catalogueId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return trainingResourceBundle;
    }
}
