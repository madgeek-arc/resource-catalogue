/**
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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.AdapterService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.OIDCSecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Comparator;
import java.util.List;

@org.springframework.stereotype.Service("adapterManager")
public class AdapterManager extends ResourceCatalogueManager<AdapterBundle> implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManager.class);
    private final OIDCSecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;

    @Value("${catalogue.id}")
    private String catalogueId;

    public AdapterManager(OIDCSecurityService securityService,
                          VocabularyService vocabularyService,
                          ProviderResourcesCommonMethods commonMethods,
                          IdCreator idCreator) {
        super(AdapterBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
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
                securityService.hasRole(auth, "ROLE_PROVIDER")) {
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
        return null;
    }

    @Override
    public boolean hasAdminAcceptedTerms(String id, boolean isDraft, Authentication authentication) {
        return AdapterService.super.hasAdminAcceptedTerms(id, isDraft, authentication);
    }

    @Override
    public void adminAcceptedTerms(String id, boolean isDraft, Authentication authentication) {
        AdapterService.super.adminAcceptedTerms(id, isDraft, authentication);
    }

    public AdapterBundle suspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        AdapterBundle adapterBundle = get(id, catalogueId, false);
        commonMethods.suspensionValidation(adapterBundle, adapterBundle.getAdapter().getCatalogueId(),
                null, suspend, auth);
        commonMethods.suspendResource(adapterBundle, suspend, auth);
        return super.update(adapterBundle, auth);
    }

    @Override
    public AdapterBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        return null;
    }

    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(AdapterBundle bundle) {
        return AdapterService.super.getLoggingInfoHistory(bundle);
    }


    @Override
    public void delete(AdapterBundle adapter) {
        if (adapter.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Adapter");
        }
        super.delete(adapter);
        logger.info("Deleted the Adapter with id '{}'", adapter.getId());
    }
}
