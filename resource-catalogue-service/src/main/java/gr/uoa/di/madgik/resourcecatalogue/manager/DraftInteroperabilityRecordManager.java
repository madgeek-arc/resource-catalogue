/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
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

@Service("draftInteroperabilityRecordManager")
public class DraftInteroperabilityRecordManager extends ResourceManager<InteroperabilityRecordBundle> implements DraftResourceService<InteroperabilityRecordBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftInteroperabilityRecordManager.class);

    private final InteroperabilityRecordService interoperabilityRecordService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftInteroperabilityRecordManager(InteroperabilityRecordService interoperabilityRecordService,
                                              IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                                              @Lazy ProviderService providerService,
                                              ProviderResourcesCommonMethods commonMethods) {
        super(InteroperabilityRecordBundle.class);
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceTypeName() {
        return "draft_interoperability_record";
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle bundle, Authentication auth) {

        bundle.setId(idCreator.generate(getResourceTypeName()));

        logger.trace("Attempting to add a new Draft Interoperability Record with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        bundle.getInteroperabilityRecord().setCatalogueId(catalogueId);
        bundle.setActive(false);
        bundle.setDraft(true);

        super.add(bundle, auth);

        return bundle;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle bundle, Authentication auth) {
        // get existing resource
        Resource existing = getDraftResource(bundle.getInteroperabilityRecord().getId());
        // block catalogueId updates from Provider Admins
        bundle.getInteroperabilityRecord().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Interoperability Record with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth)));
        // save existing resource with new payload
        existing.setPayload(serialize(bundle));
        existing.setResourceType(getResourceType());
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Interoperability Record: {}", bundle);
        return bundle;
    }

    @Override
    public void delete(InteroperabilityRecordBundle bundle) {
        super.delete(bundle);
    }

    @Override
    public InteroperabilityRecordBundle transformToNonDraft(String id, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = this.get(id);
        return transformToNonDraft(interoperabilityRecordBundle, auth);
    }

    @Override
    public InteroperabilityRecordBundle transformToNonDraft(InteroperabilityRecordBundle bundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Interoperability Record with id {} to Active", bundle.getId());
        interoperabilityRecordService.validate(bundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        bundle.setStatus("pending interoperability record");
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        bundle.setDraft(false);

        ResourceType guidelinesResourceType = resourceTypeService.getResourceType("interoperability_record");
        Resource resource = getDraftResource(bundle.getId());
        resource.setResourceType(getResourceType());
        resourceService.changeResourceType(resource, guidelinesResourceType);

        try {
            bundle = interoperabilityRecordService.update(bundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return bundle;
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

    private Resource getDraftResource(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        getResourceTypeName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

}
