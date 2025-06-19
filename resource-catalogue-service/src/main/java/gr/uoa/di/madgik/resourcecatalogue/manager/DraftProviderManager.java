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

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("draftProviderManager")
public class DraftProviderManager extends ResourceCatalogueManager<ProviderBundle> implements DraftResourceService<ProviderBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftProviderManager.class);

    private final ProviderService providerManager;
    private final IdCreator idCreator;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftProviderManager(ProviderService providerManager,
                                IdCreator idCreator, @Lazy RegistrationMailService registrationMailService,
                                @Lazy VocabularyService vocabularyService,
                                ProviderResourcesCommonMethods commonMethods) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
    }


    @Override
    public String getResourceTypeName() {
        return "provider";
    }

    @Override
    public ProviderBundle add(ProviderBundle bundle, Authentication auth) {

        bundle.setId(idCreator.generate(getResourceTypeName()));
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);
        commonMethods.addAuthenticatedUser(bundle.getProvider(), auth);

        logger.trace("Attempting to add a new Draft Provider: {}", bundle);
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        bundle.getProvider().setCatalogueId(catalogueId);
        bundle.setActive(false);
        bundle.setDraft(true);

        super.add(bundle, auth);

        return bundle;
    }

    @Override
    public ProviderBundle update(ProviderBundle bundle, Authentication auth) {
        // get existing resource
        Resource existing = getResource(bundle.getId());
        // block catalogueId updates from Provider Admins
        bundle.getProvider().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Provider: {}", bundle);
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        // save existing resource with new payload
        existing.setPayload(serialize(bundle));
        existing.setResourceType(getResourceType());
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Provider: {}", bundle);
        return bundle;
    }

    @Override
    public void delete(ProviderBundle bundle) {
        super.delete(bundle);
    }

    @Override
    public ProviderBundle transformToNonDraft(String id, Authentication auth) {
        ProviderBundle providerBundle = get(id, catalogueId, false);
        return transformToNonDraft(providerBundle, auth);
    }

    @Override
    public ProviderBundle transformToNonDraft(ProviderBundle bundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Provider with id '{}' to Provider", bundle.getId());
        providerManager.validate(bundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfo);

        // update providerStatus
        bundle.setStatus(vocabularyService.get("pending provider").getId());
        bundle.setTemplateStatus(vocabularyService.get("no template status").getId());

        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        bundle.setDraft(false);

        try {
            bundle = providerManager.update(bundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.info("Provider with id '{}' does not exist", bundle.getId());
        }

        registrationMailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null);
        return bundle;
    }
}
