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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.Comparator;
import java.util.List;

@org.springframework.stereotype.Service("interoperabilityRecordManager")
public class InteroperabilityRecordManager extends ResourceCatalogueManager<InteroperabilityRecordBundle> implements InteroperabilityRecordService {

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
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) { // add catalogue provider
            interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(this.catalogueId);
            interoperabilityRecordBundle.setId(idCreator.generate(getResourceTypeName()));
            commonMethods.createIdentifiers(interoperabilityRecordBundle, getResourceTypeName(), false);
        } else { // external catalogue
            commonMethods.checkCatalogueIdConsistency(interoperabilityRecordBundle, catalogueId);
            idCreator.validateId(interoperabilityRecordBundle.getId());
            commonMethods.createIdentifiers(interoperabilityRecordBundle, getResourceTypeName(), true);
        }
        logger.trace("Attempting to add a new Interoperability Record: {}", interoperabilityRecordBundle.getInteroperabilityRecord());

        ProviderBundle providerBundle = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().
                getCatalogueId(), interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
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
        interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfoList.getFirst());
        interoperabilityRecordBundle.setAuditState(Auditable.NOT_AUDITED);

        if (!providerBundle.getProvider().getCatalogueId().equals(this.catalogueId)) {
            interoperabilityRecordBundle.setStatus(vocabularyService.get("approved interoperability record").getId());
            interoperabilityRecordBundle.setActive(true);
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            interoperabilityRecordBundle.setLatestOnboardingInfo(loggingInfoApproved);
        }
        interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        interoperabilityRecordBundle.getInteroperabilityRecord().setCreated(new LocalDate().toString());
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
        existingInteroperabilityRecord = get(ret.getInteroperabilityRecord().getId(), ret.getInteroperabilityRecord().getCatalogueId(), false);
        if (ret.getInteroperabilityRecord().equals(existingInteroperabilityRecord.getInteroperabilityRecord())) {
            return ret;
        }

        if (catalogueId == null || catalogueId.isEmpty()) {
            ret.getInteroperabilityRecord().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }

        validate(ret);

        // block Public Interoperability Record update
        if (existingInteroperabilityRecord.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Interoperability Record");
        }

        // update existing InteroperabilityRecord Metadata, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingInteroperabilityRecord.getMetadata(), AuthenticationInfo.getFullName(auth)));
        ret.setIdentifiers(existingInteroperabilityRecord.getIdentifiers());
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
        ret.getInteroperabilityRecord().setUpdated(new LocalDate().toString());

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingInteroperabilityRecord.getInteroperabilityRecord().getCatalogueId().equals(ret.getInteroperabilityRecord().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        Resource existing = getResource(ret.getInteroperabilityRecord().getId(),
                ret.getInteroperabilityRecord().getCatalogueId(), false);
        if (existing == null) {
            throw new ResourceNotFoundException(ret.getId(), "Interoperability Record");
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
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id, catalogueId, false);
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
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id, catalogueId, false);
        String activeProvider = "";

        ProviderBundle providerBundle = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
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
        return super.validate(interoperabilityRecordBundle);
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

    public InteroperabilityRecordBundle getOrElseReturnNull(String id) {
        InteroperabilityRecordBundle interoperabilityRecordBundle;
        try {
            interoperabilityRecordBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return interoperabilityRecordBundle;
    }

    @Override
    public Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider_id", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth);
    }

    public InteroperabilityRecordBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(id, catalogueId, false);
        ProviderBundle provider = providerService.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), auth);
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


    public InteroperabilityRecordBundle suspend(String interoperabilityRecordId, String catalogueId, boolean suspend, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = get(interoperabilityRecordId, catalogueId, false);
        commonMethods.suspensionValidation(interoperabilityRecordBundle, interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), suspend, auth);
        commonMethods.suspendResource(interoperabilityRecordBundle, suspend, auth);
        return super.update(interoperabilityRecordBundle, auth);
    }

    //FIXME: find a better way to get EOSC Monitoring IG - title is not unique
    public InteroperabilityRecordBundle getEOSCMonitoringGuideline() {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.addFilter("title", "EOSC Monitoring: Architecture and Interoperability Guidelines");
        List<InteroperabilityRecordBundle> igList = getAll(ff).getResults();
        if (!igList.isEmpty()) {
            return igList.getFirst();
        }
        throw new CatalogueResourceNotFoundException("Could not find EOSC Monitoring Guideline");
    }
}
