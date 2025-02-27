/**
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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@org.springframework.stereotype.Service("interoperabilityRecordManager")
public class InteroperabilityRecordManager extends ResourceManager<InteroperabilityRecordBundle> implements InteroperabilityRecordService {

    private static final Logger logger = LoggerFactory.getLogger(InteroperabilityRecordManager.class);
    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordManager;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public InteroperabilityRecordManager(ProviderService providerService, IdCreator idCreator,
                                         SecurityService securityService, VocabularyService vocabularyService,
                                         PublicInteroperabilityRecordService publicInteroperabilityRecordManager,
                                         RegistrationMailService registrationMailService,
                                         ProviderResourcesCommonMethods commonMethods) {
        super(InteroperabilityRecordBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceTypeName() {
        return "interoperability_record";
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        return add(interoperabilityRecordBundle, null, auth);
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.isEmpty()) { // add catalogue provider
            interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(this.catalogueId);
        } else { // external catalogue
            commonMethods.checkCatalogueIdConsistency(interoperabilityRecordBundle, catalogueId);
        }
        logger.trace("Attempting to add a new Interoperability Record: {}", interoperabilityRecordBundle.getInteroperabilityRecord());
        interoperabilityRecordBundle.setId(idCreator.generate(getResourceTypeName()));

        // register and ensure Resource Catalogue's PID uniqueness
        commonMethods.determineResourceAndCreateAlternativeIdentifierForPID(interoperabilityRecordBundle, getResourceTypeName());
        interoperabilityRecordBundle.getInteroperabilityRecord().setAlternativeIdentifiers(
                commonMethods.ensureResourceCataloguePidUniqueness(interoperabilityRecordBundle.getId(),
                        interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                        interoperabilityRecordBundle.getInteroperabilityRecord().getAlternativeIdentifiers()));

        ProviderBundle providerBundle = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ResourceException(String.format("The Provider ID '%s' you provided is not yet approved",
                    interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId()), HttpStatus.CONFLICT);
        }
        validate(interoperabilityRecordBundle);

        // status
        interoperabilityRecordBundle.setStatus("pending interoperability record");
        // metadata
        if (interoperabilityRecordBundle.getMetadata() == null) {
            interoperabilityRecordBundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth)));
        }
        // loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(interoperabilityRecordBundle, auth);
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);
        interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfoList.getFirst());
        interoperabilityRecordBundle.setAuditState(Auditable.NOT_AUDITED);

        interoperabilityRecordBundle.getInteroperabilityRecord().setCreated(String.valueOf(System.currentTimeMillis()));
        interoperabilityRecordBundle.getInteroperabilityRecord().setUpdated(interoperabilityRecordBundle.getInteroperabilityRecord().getCreated());
        logger.info("Added a new Interoperability Record with id '{}' and title '{}'", interoperabilityRecordBundle.getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getTitle());
        super.add(interoperabilityRecordBundle, auth);
        registrationMailService.sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(interoperabilityRecordBundle, User.of(auth));

        return interoperabilityRecordBundle;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        return update(interoperabilityRecordBundle, interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), auth);
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId, Authentication auth) {
        logger.trace("Attempting to update the Interoperability Record with id '{}'", interoperabilityRecordBundle.getId());

        InteroperabilityRecordBundle ret = ObjectUtils.clone(interoperabilityRecordBundle);
        InteroperabilityRecordBundle existingInteroperabilityRecord;
        try {
            existingInteroperabilityRecord = get(ret.getInteroperabilityRecord().getId(), ret.getInteroperabilityRecord().getCatalogueId());
            if (ret.getInteroperabilityRecord().equals(existingInteroperabilityRecord.getInteroperabilityRecord())) {
                return ret;
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Interoperability Record with id [%s] on the [%s] Catalogue",
                    ret.getInteroperabilityRecord().getId(), ret.getInteroperabilityRecord().getCatalogueId()));
        }

        if (catalogueId == null || catalogueId.isEmpty()) {
            ret.getInteroperabilityRecord().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }

        // ensure Resource Catalogue's PID uniqueness
        if (ret.getInteroperabilityRecord().getAlternativeIdentifiers() == null ||
                ret.getInteroperabilityRecord().getAlternativeIdentifiers().isEmpty()) {
            commonMethods.determineResourceAndCreateAlternativeIdentifierForPID(ret, getResourceTypeName());
        } else {
            ret.getInteroperabilityRecord().setAlternativeIdentifiers(
                    commonMethods.ensureResourceCataloguePidUniqueness(ret.getId(),
                            ret.getInteroperabilityRecord().getCatalogueId(),
                            ret.getInteroperabilityRecord().getAlternativeIdentifiers()));
        }

        validate(ret);

        // block Public Interoperability Record update
        if (existingInteroperabilityRecord.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Interoperability Record");
        }

        // update existing InteroperabilityRecord Metadata, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingInteroperabilityRecord.getMetadata(), AuthenticationInfo.getFullName(auth)));
        ret.setMigrationStatus(existingInteroperabilityRecord.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingInteroperabilityRecord, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // active/status
        ret.setActive(existingInteroperabilityRecord.isActive());
        ret.setStatus(existingInteroperabilityRecord.getStatus());
        ret.setSuspended(existingInteroperabilityRecord.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));

        // updated && created
        ret.getInteroperabilityRecord().setCreated(existingInteroperabilityRecord.getInteroperabilityRecord().getCreated());
        ret.getInteroperabilityRecord().setUpdated(String.valueOf(System.currentTimeMillis()));

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingInteroperabilityRecord.getInteroperabilityRecord().getCatalogueId().equals(ret.getInteroperabilityRecord().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        Resource existing = getResource(ret.getInteroperabilityRecord().getId(), ret.getInteroperabilityRecord().getCatalogueId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Interoperability Record with id '%s' because it does not exist",
                            ret.getInteroperabilityRecord().getId()));
        }
        existing.setPayload(serialize(ret));
        existing.setResourceType(getResourceType());

        resourceService.updateResource(existing);
        logger.info("Updated Interoperability Record with id '{}' and title '{}'", ret.getId(),
                ret.getInteroperabilityRecord().getTitle());

        return ret;
    }

    @Override
    public void delete(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        // block Public InteroperabilityRecordBundle deletions
        if (interoperabilityRecordBundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Interoperability Record");
        }
        super.delete(interoperabilityRecordBundle);
        logger.info("Deleted the Interoperability Record with id '{}'", interoperabilityRecordBundle.getId());
    }


    public InteroperabilityRecordBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Interoperability Record state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist an Interoperability Record state!", status));
        }
        logger.trace("verifyResource with id: '{}' | status: '{}' | active: '{}'", id, status, active);
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id);
        interoperabilityRecordBundle.setStatus(vocabularyService.get(status).getId());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(interoperabilityRecordBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "approved interoperability record":
                interoperabilityRecordBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            case "rejected interoperability record":
                interoperabilityRecordBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            case "pending interoperability record":
            default:
                break;
        }

        logger.info("User '{}' verified Interoperability Record with id: '{}' | status: '{}' | active: '{}'",
                auth, interoperabilityRecordBundle.getId(), status, active);
        registrationMailService.sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(interoperabilityRecordBundle,
                User.of(auth));
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
        if (active && activeProvider.isEmpty()) {
            throw new ResourceException("Interoperability Record does not have active Providers", HttpStatus.CONFLICT);
        }
        interoperabilityRecordBundle.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(interoperabilityRecordBundle, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        interoperabilityRecordBundle.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        interoperabilityRecordBundle.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        interoperabilityRecordBundle.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));


        super.update(interoperabilityRecordBundle, auth);
        logger.info("User '{}-{}' saved Interoperability Record with id '{}' as '{}'",
                AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase(),
                id, active);
        return interoperabilityRecordBundle;
    }

    @Override
    public InteroperabilityRecordBundle validate(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        logger.debug("Validating InteroperabilityRecord with id: '{}'", interoperabilityRecordBundle.getId());
        super.validate(interoperabilityRecordBundle);
        return interoperabilityRecordBundle;
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        filter.addFilter("provider_id", providers.stream().map(ProviderBundle::getId).toList());
        filter.setResourceType(getResourceTypeName());
        return this.getAll(filter, auth);
    }

    // TODO: refactor
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle;
        try {
            interoperabilityRecordBundle = get(id, catalogueId);
            Resource resource = getResource(interoperabilityRecordBundle.getInteroperabilityRecord().getId()); // get all versions

            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            InteroperabilityRecordBundle interoperabilityRecordResource = deserialize(resource);
            if (interoperabilityRecordResource.getLoggingInfo() != null) {
                loggingInfoList.addAll(interoperabilityRecordResource.getLoggingInfo());
            }

            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info("Interoperability Record with id '{}' not found", id);
        }
        return null;
    }

    public InteroperabilityRecordBundle getOrElseReturnNull(String id) {
        InteroperabilityRecordBundle interoperabilityRecordBundle;
        try {
            interoperabilityRecordBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
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

    @Override
    public Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider_id", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth);
    }

    public InteroperabilityRecordBundle audit(String id, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id);
        ProviderBundle provider = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
        commonMethods.auditResource(interoperabilityRecordBundle, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            interoperabilityRecordBundle.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            interoperabilityRecordBundle.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(interoperabilityRecordBundle, provider.getProvider().getUsers());

        logger.info("User '{}-{}' audited Interoperability Record '{}'-'{}' with [actionType: {}]",
                AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), actionType);
        return super.update(interoperabilityRecordBundle, auth);
    }

    public InteroperabilityRecordBundle createPublicInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth) {
        publicInteroperabilityRecordManager.add(interoperabilityRecordBundle, auth);
        return interoperabilityRecordBundle;
    }


    public InteroperabilityRecordBundle suspend(String interoperabilityRecordId, boolean suspend, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(interoperabilityRecordId);
        commonMethods.suspensionValidation(interoperabilityRecordBundle, interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), suspend, auth);
        commonMethods.suspendResource(interoperabilityRecordBundle, suspend, auth);
        return super.update(interoperabilityRecordBundle, auth);
    }
}
