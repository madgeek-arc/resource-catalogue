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
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@org.springframework.stereotype.Service("interoperabilityRecordManager")
public class InteroperabilityRecordManager extends ResourceManager<InteroperabilityRecordBundle> implements InteroperabilityRecordService<InteroperabilityRecordBundle> {

    private static final Logger logger = LogManager.getLogger(InteroperabilityRecordManager.class);
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final PublicInteroperabilityRecordManager publicInteroperabilityRecordManager;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;
    @Autowired
    private FieldValidator fieldValidator;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public InteroperabilityRecordManager(ProviderService<ProviderBundle, Authentication> providerService, IdCreator idCreator,
                                         SecurityService securityService, VocabularyService vocabularyService,
                                         PublicInteroperabilityRecordManager publicInteroperabilityRecordManager,
                                         CatalogueService<CatalogueBundle, Authentication> catalogueService,
                                         RegistrationMailService registrationMailService, ProviderResourcesCommonMethods commonMethods) {
        super(InteroperabilityRecordBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.catalogueService = catalogueService;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "interoperability_record";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddResources(#auth, #interoperabilityRecordBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        return add(interoperabilityRecordBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddResources(#auth, #interoperabilityRecordBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.equals("")) { // add catalogue provider
            interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(catalogueName);
        } else { // external catalogue
            commonMethods.checkCatalogueIdConsistency(interoperabilityRecordBundle, catalogueId);
        }

        ProviderBundle providerBundle = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ValidationException(String.format("The Provider ID '%s' you provided is not yet approved",
                    interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId()));
        }
        try {
            interoperabilityRecordBundle.getInteroperabilityRecord().setId(idCreator.createInteroperabilityRecordId(interoperabilityRecordBundle.getInteroperabilityRecord()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        validate(interoperabilityRecordBundle);

        // status
        interoperabilityRecordBundle.setStatus("pending interoperability record");
        // metadata
        if (interoperabilityRecordBundle.getMetadata() == null) {
            interoperabilityRecordBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        // loggingInfo
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);
        interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfo);

        interoperabilityRecordBundle.getInteroperabilityRecord().setCreated(String.valueOf(System.currentTimeMillis()));
        interoperabilityRecordBundle.getInteroperabilityRecord().setUpdated(interoperabilityRecordBundle.getInteroperabilityRecord().getCreated());
        logger.trace("User '{}' is attempting to add a new Interoperability Record: {}", auth, interoperabilityRecordBundle.getInteroperabilityRecord());
        logger.info("Adding Interoperability Record: {}", interoperabilityRecordBundle.getInteroperabilityRecord());
        super.add(interoperabilityRecordBundle, auth);
        registrationMailService.sendEmailsForInteroperabilityRecordOnboarding(interoperabilityRecordBundle, User.of(auth));

        return interoperabilityRecordBundle;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isResourceProviderAdmin(#auth, #interoperabilityRecordBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        return update(interoperabilityRecordBundle, interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isResourceProviderAdmin(#auth, #interoperabilityRecordBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Interoperability Record with id '{}'", auth, interoperabilityRecordBundle.getId());

        InteroperabilityRecordBundle existingInteroperabilityRecord;
        try {
            existingInteroperabilityRecord = get(interoperabilityRecordBundle.getInteroperabilityRecord().getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Interoperability Record with id [%s] on the [%s] Catalogue",
                    interoperabilityRecordBundle.getInteroperabilityRecord().getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        }

        // FIXME: DOES NOT WORK
        // check if there are actual changes in the InteroperabilityRecord
        if (interoperabilityRecordBundle.getInteroperabilityRecord().equals(existingInteroperabilityRecord.getInteroperabilityRecord())){
            throw new ValidationException("There are no changes in the Interoperability Record", HttpStatus.OK);
        }

        if (catalogueId == null || catalogueId.equals("")) {
            interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(catalogueName);
        } else {
            commonMethods.checkCatalogueIdConsistency(interoperabilityRecordBundle, catalogueId);
        }

        validate(interoperabilityRecordBundle);

        // block Public Training Resource update
        if (existingInteroperabilityRecord.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Interoperability Record");
        }

        User user = User.of(auth);
        // update existing TrainingResource Metadata, MigrationStatus
        interoperabilityRecordBundle.setMetadata(Metadata.updateMetadata(existingInteroperabilityRecord.getMetadata(), user.getFullName()));
        interoperabilityRecordBundle.setMigrationStatus(existingInteroperabilityRecord.getMigrationStatus());

        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        if (existingInteroperabilityRecord.getLoggingInfo() != null) {
            loggingInfoList = existingInteroperabilityRecord.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        } else {
            loggingInfoList.add(loggingInfo);
        }
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        interoperabilityRecordBundle.setLatestUpdateInfo(loggingInfo);
        interoperabilityRecordBundle.setActive(existingInteroperabilityRecord.isActive());

        // status
        interoperabilityRecordBundle.setStatus(existingInteroperabilityRecord.getStatus());

        // updated && created
        interoperabilityRecordBundle.getInteroperabilityRecord().setCreated(existingInteroperabilityRecord.getInteroperabilityRecord().getCreated());
        interoperabilityRecordBundle.getInteroperabilityRecord().setUpdated(String.valueOf(System.currentTimeMillis()));

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingInteroperabilityRecord.getInteroperabilityRecord().getCatalogueId().equals(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        Resource existing = getResource(interoperabilityRecordBundle.getInteroperabilityRecord().getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Interoperability Record with id '%s' because it does not exist",
                            interoperabilityRecordBundle.getInteroperabilityRecord().getId()));
        }
        existing.setPayload(serialize(interoperabilityRecordBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
        logger.debug("Updating Interoperability Record: {}", interoperabilityRecordBundle);

        return interoperabilityRecordBundle;
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Interoperability Record state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist an Interoperability Record state!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id);
        interoperabilityRecordBundle.setStatus(vocabularyService.get(status).getId());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        User user = User.of(auth);

        if (interoperabilityRecordBundle.getLoggingInfo() != null) {
            loggingInfoList = interoperabilityRecordBundle.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "pending interoperability record":
                break;
            case "approved interoperability record":
                interoperabilityRecordBundle.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            case "rejected interoperability record":
                interoperabilityRecordBundle.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            default:
                break;
        }
        logger.info("Verifying Interoperability Record: {}", interoperabilityRecordBundle);
        registrationMailService.sendEmailsForInteroperabilityRecordOnboarding(interoperabilityRecordBundle, User.of(auth));
        return super.update(interoperabilityRecordBundle, auth);
    }

    @Override
    public InteroperabilityRecordBundle publish(String id, Boolean active, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id);
        String activeProvider = "";

        ProviderBundle providerBundle = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Interoperability Record does not have active Providers", HttpStatus.CONFLICT);
        }
        interoperabilityRecordBundle.setActive(active);

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
        if (interoperabilityRecordBundle.getLoggingInfo() != null) {
            loggingInfoList = interoperabilityRecordBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
            loggingInfoList.add(loggingInfo);
        }
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        interoperabilityRecordBundle.setLatestUpdateInfo(loggingInfo);

        update(interoperabilityRecordBundle, auth);
        return interoperabilityRecordBundle;
    }

    public boolean validateInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        validate(interoperabilityRecordBundle);
        return true;
    }

    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = new InteroperabilityRecordBundle();
        try {
            interoperabilityRecordBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(interoperabilityRecordBundle.getInteroperabilityRecord().getId()); // get all versions
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources) {
                InteroperabilityRecordBundle interoperabilityRecordResource = deserialize(resource);
                if (interoperabilityRecordResource.getLoggingInfo() != null) {
                    loggingInfoList.addAll(interoperabilityRecordResource.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info(String.format("Interoperability Record with id [%s] not found", id));
        }
        return null;
    }

    public List<Resource> getResources(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("%s_id = \"%s\"", resourceType.getName(), id),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources != null) {
            return resources.getResults();
        }
        return Collections.emptyList();
    }

    public InteroperabilityRecordBundle getOrElseReturnNull(String id, String catalogueId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle;
        try {
            interoperabilityRecordBundle = get(id, catalogueId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return interoperabilityRecordBundle;
    }

    public InteroperabilityRecordBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Interoperability Record with id: %s and catalogueId: %s", id, catalogueId));
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

    @Override
    public Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider_id", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth);
    }

    public InteroperabilityRecordBundle getCatalogueInteroperabilityRecord(String catalogueId, String interoperabilityRecordId, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(interoperabilityRecordId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (interoperabilityRecordBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find InteroperabilityRecord with id: %s", interoperabilityRecordId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId().equals(catalogueId)) {
            throw new ValidationException(String.format("Interoperability Record with id [%s] does not belong to the catalogue with id [%s]", interoperabilityRecordId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceProviderAdmin(user, interoperabilityRecordId, catalogueId)) {
                return interoperabilityRecordBundle;
            }
        }
        // else return the Interoperability Record ONLY if it is active
        if (interoperabilityRecordBundle.getStatus().equals(vocabularyService.get("approved interoperability record").getId())) {
            return interoperabilityRecordBundle;
        }
        throw new ValidationException("You cannot view the specific Interoperability Record");
    }

    public InteroperabilityRecordBundle createPublicInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth){
        publicInteroperabilityRecordManager.add(interoperabilityRecordBundle, auth);
        return interoperabilityRecordBundle;
    }

    public FacetFilter createFacetFilterForFetchingInteroperabilityRecords(MultiValueMap<String, Object> allRequestParams, String catalogueId, String providerId){
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        allRequestParams.remove("catalogue_id");
        allRequestParams.remove("provider_id");
        if (catalogueId != null){
            if (!catalogueId.equals("all")){
                ff.addFilter("catalogue_id", catalogueId);
            }
        }
        if (providerId != null){
            if (!providerId.equals("all")){
                ff.addFilter("provider_id", providerId);
            }
        }
        ff.addFilter("published", false);
        ff.setResourceType("interoperability_record");
        return ff;
    }

    public void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth){
        // if user is Unauthorized, return active/latest ONLY
        if (auth == null) {
            filter.addFilter("active", true);
        }
        if (auth != null && auth.isAuthenticated()) {
            // if user is Authorized with ROLE_USER, return active/latest ONLY
            if (!securityService.hasRole(auth, "ROLE_PROVIDER") && !securityService.hasRole(auth, "ROLE_EPOT") &&
                    !securityService.hasRole(auth, "ROLE_ADMIN")) {
                filter.addFilter("active", true);
            }
        }
    }
}
