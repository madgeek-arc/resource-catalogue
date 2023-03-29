package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

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
    @Autowired
    private FieldValidator fieldValidator;

    public InteroperabilityRecordManager(ProviderService<ProviderBundle, Authentication> providerService, IdCreator idCreator,
                                         SecurityService securityService, VocabularyService vocabularyService) {
        super(InteroperabilityRecordBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
    }

    @Override
    public String getResourceType() {
        return "interoperability_record";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or " + "@securityService.isResourceProviderAdmin(#auth, #interoperabilityRecordBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
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

        return interoperabilityRecordBundle;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or " + "@securityService.isResourceProviderAdmin(#auth, #interoperabilityRecordBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Interoperability Record with id '{}'", auth, interoperabilityRecordBundle.getId());

        Resource existing = whereID(interoperabilityRecordBundle.getId(), true);
        InteroperabilityRecordBundle ex = deserialize(existing);
        // check if there are actual changes in the InteroperabilityRecord
        if (interoperabilityRecordBundle.getInteroperabilityRecord().equals(ex.getInteroperabilityRecord())){
            throw new ValidationException("There are no changes in the Interoperability Record", HttpStatus.OK);
        }

        validate(interoperabilityRecordBundle);

        // block Public Training Resource update
        if (ex.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Interoperability Record");
        }

        User user = User.of(auth);
        // update existing TrainingResource Metadata, Identifiers, MigrationStatus
        interoperabilityRecordBundle.setMetadata(Metadata.updateMetadata(interoperabilityRecordBundle.getMetadata(), user.getFullName()));
        interoperabilityRecordBundle.setMigrationStatus(interoperabilityRecordBundle.getMigrationStatus());

        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        if (ex.getLoggingInfo() != null) {
            loggingInfoList = ex.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        } else {
            loggingInfoList.add(loggingInfo);
        }
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        interoperabilityRecordBundle.setLatestUpdateInfo(loggingInfo);
        interoperabilityRecordBundle.setActive(ex.isActive());

        // status
        interoperabilityRecordBundle.setStatus(interoperabilityRecordBundle.getStatus());

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!ex.getInteroperabilityRecord().getCatalogueId().equals(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        interoperabilityRecordBundle.getInteroperabilityRecord().setCreated(ex.getInteroperabilityRecord().getCreated());
        interoperabilityRecordBundle.getInteroperabilityRecord().setUpdated(String.valueOf(System.currentTimeMillis()));
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
        return update(interoperabilityRecordBundle, auth);
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

    public Paging<LoggingInfo> getLoggingInfoHistory(String id) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = new InteroperabilityRecordBundle();
        try {
            interoperabilityRecordBundle = get(id);
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

    public InteroperabilityRecordBundle createPublicInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth){
//        publicInteroperabilityRecordManager.add(interoperabilityRecordBundle, auth);
        return interoperabilityRecordBundle;
    }
}
