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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service("adapterManager")
public class AdapterManager extends ResourceCatalogueManager<AdapterBundle> implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManager.class);
    private final OIDCSecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;
    private final ServiceBundleService serviceBundleService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ProviderService providerService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public AdapterManager(OIDCSecurityService securityService,
                          VocabularyService vocabularyService,
                          ProviderResourcesCommonMethods commonMethods,
                          IdCreator idCreator, ServiceBundleService serviceBundleService,
                          InteroperabilityRecordService interoperabilityRecordService,
                          ProviderService providerService) {
        super(AdapterBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
        this.serviceBundleService = serviceBundleService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.providerService = providerService;
    }

    @Override
    public String getResourceTypeName() {
        return "adapter";
    }

    @Override
    public AdapterBundle add(AdapterBundle adapter, Authentication authentication) {
        return add(adapter, null, authentication);
    }

    @Override
    public AdapterBundle add(AdapterBundle adapter, String catalogueId, Authentication auth) {
        logger.trace("Attempting to add a new Adapter: {} on Catalogue: '{}'", adapter, catalogueId);

        adapter = onboard(adapter, auth);
        commonMethods.addAuthenticatedUser(adapter.getAdapter(), auth);

        validate(adapter);
        adapter.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase()));

        AdapterBundle ret;
        ret = super.add(adapter, null);
        logger.debug("Adding Adapter: {} of Catalogue: '{}'", adapter, catalogueId);

        return ret;
    }

    @Override
    public AdapterBundle update(AdapterBundle adapter, String comment, Authentication auth) {
        return update(adapter, adapter.getAdapter().getCatalogueId(), comment, auth);
    }

    @Override
    public AdapterBundle update(AdapterBundle adapterBundle, String catalogueId, String comment, Authentication auth) {
        logger.trace("Attempting to update the Adapter with id '{}' of the Catalogue '{}'", adapterBundle,
                adapterBundle.getAdapter().getCatalogueId());

        AdapterBundle ret = ObjectUtils.clone(adapterBundle);
        Resource existingResource = getResource(ret.getId(), ret.getAdapter().getCatalogueId(), false);
        AdapterBundle existingAdapter = deserialize(existingResource);
        // check if there are actual changes in the Adapter
        if (ret.getAdapter().equals(existingAdapter.getAdapter())) {
            if (ret.isSuspended() == existingAdapter.isSuspended()) {
                return ret;
            }
        }

        //TODO: revisit if we want Adapters for external Catalogues
        ret.getAdapter().setCatalogueId(this.catalogueId);

        // block Public Adapter update
        if (ret.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Adapter");
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // block catalogueId updates from Adapter Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN") &&
                !existingAdapter.getAdapter().getCatalogueId().equals(ret.getAdapter().getCatalogueId())) {
            throw new ValidationException("You cannot change catalogueId");
        }
        ret.setIdentifiers(existingAdapter.getIdentifiers());
        ret.setActive(existingAdapter.isActive());
        ret.setStatus(existingAdapter.getStatus());
        ret.setSuspended(existingAdapter.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));
        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(getResourceType());
        resourceService.updateResource(existingResource);
        logger.debug("Updating Adapter: {} of Catalogue: {}", ret, ret.getAdapter().getCatalogueId());

        return ret;
    }

    //TODO: revisit if we want Adapters for external Catalogues
    private AdapterBundle onboard(AdapterBundle adapter, Authentication auth) {
        // create LoggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(adapter, auth);
        adapter.setLoggingInfo(loggingInfoList);
        adapter.getAdapter().setCatalogueId(this.catalogueId);

        if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                getProviderUserEmails().contains(AuthenticationInfo.getEmail(auth).toLowerCase())) {
            adapter.setActive(true);
            adapter.setStatus(vocabularyService.get("approved adapter").getId());
        } else if (securityService.hasRole(auth, "ROLE_USER")) {
            adapter.setActive(false);
            adapter.setStatus(vocabularyService.get("pending adapter").getId());
        } else {
            throw new AccessDeniedException("You do not have permission to perform this action");
        }
        adapter.setId(idCreator.generate(getResourceTypeName()));
        commonMethods.createIdentifiers(adapter, getResourceTypeName(), false);
        adapter.setAuditState(Auditable.NOT_AUDITED);
        adapter.setLatestOnboardingInfo(loggingInfoList.getLast());

        return adapter;
    }

    @Override
    public Browsing<AdapterBundle> getMy(FacetFilter ff, Authentication auth) {
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
        ff.addFilter("admins", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth);
    }

    @Override
    public AdapterBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Adapter state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Adapter State!", status));
        }
        logger.trace("verify adapter with id: '{}' | status: '{}' | active: '{}'", id, status, active);
        AdapterBundle adapter = get(id, catalogueId, false);
        Resource existingResource = getResource(adapter.getId(), adapter.getAdapter().getCatalogueId(), false);
        AdapterBundle existingAdapter = deserialize(existingResource);

        existingAdapter.setStatus(vocabularyService.get(status).getId());
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingAdapter, auth);
        LoggingInfo loggingInfo = null;

        switch (status) {
            case "approved adapter":
                existingAdapter.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                break;
            case "rejected adapter":
                existingAdapter.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                break;
            default:
                break;
        }
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        existingAdapter.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        existingAdapter.setLatestOnboardingInfo(loggingInfo);

        logger.info("Verifying ADapter: {}", existingAdapter);
        existingResource.setPayload(serialize(existingAdapter));
        existingResource.setResourceType(getResourceType());
        resourceService.updateResource(existingResource);
        return existingAdapter;
    }

    @Override
    public AdapterBundle publish(String id, Boolean active, Authentication auth) {
        AdapterBundle adapterBundle = get(id, catalogueId, false);
        adapterBundle.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(adapterBundle, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        adapterBundle.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        adapterBundle.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        adapterBundle.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        adapterBundle.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));


        super.update(adapterBundle, auth);
        logger.info("User '{}-{}' saved Adapter with id '{}' as '{}'",
                AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase(),
                id, active);
        return adapterBundle;
    }

    @Override
    public boolean hasAdminAcceptedTerms(String id, boolean isDraft, Authentication authentication) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void adminAcceptedTerms(String id, boolean isDraft, Authentication authentication) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AdapterBundle suspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        AdapterBundle adapterBundle = get(id, catalogueId, false);
        commonMethods.suspensionValidation(adapterBundle, adapterBundle.getAdapter().getCatalogueId(),
                null, suspend, auth);
        commonMethods.suspendResource(adapterBundle, suspend, auth);
        return super.update(adapterBundle, auth);
    }

    @Override
    public AdapterBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        AdapterBundle adapterBundle = get(id, catalogueId, false);
        commonMethods.auditResource(adapterBundle, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            adapterBundle.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            adapterBundle.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }

        logger.info("User '{}-{}' audited Adapter '{}'-'{}' with [actionType: {}]",
                AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase(),
                adapterBundle.getAdapter().getId(),
                adapterBundle.getAdapter().getName(), actionType);
        return super.update(adapterBundle, auth);
    }

    @Override
    public Paging<AdapterBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void delete(AdapterBundle adapter) {
        if (adapter.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Adapter");
        }
        super.delete(adapter);
        logger.info("Deleted the Adapter with id '{}'", adapter.getId());
    }

    @Override
    public AdapterBundle validate(AdapterBundle adapter) {
        validateLinkedResource(adapter.getAdapter().getLinkedResource());
        return super.validate(adapter);
    }

    private void validateLinkedResource(LinkedResource linkedResource) {
        String type = linkedResource.getType();
        String id = linkedResource.getId();

        switch (type) {
            case "Guideline":
                interoperabilityRecordService.get(id, catalogueId, false);
                break;
            case "Service":
                serviceBundleService.get(id, catalogueId, false);
                break;
            default:
                throw new ValidationException("Unsupported linked resource type: [" + type + "]. " +
                        "Supported types are [Guideline, Service]");
        }
    }

    private Set<String> getProviderUserEmails() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providers = providerService.getAll(ff, securityService.getAdminAccess()).getResults();
        return providers
                .stream()
                .flatMap(p -> (p.getProvider().getUsers() != null ? p.getProvider().getUsers() : new ArrayList<User>())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(User::getEmail)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase))
                .filter(u -> u != null && !Objects.equals("", u))
                .collect(Collectors.toSet());
    }
}
