/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service("interoperabilityRecordManager")
public class InteroperabilityRecordManager extends ResourceCatalogueGenericManager<InteroperabilityRecordBundle>
        implements InteroperabilityRecordService {

    private static final Logger logger = LoggerFactory.getLogger(InteroperabilityRecordManager.class);

    private static final String RESERVED_NAME = "EOSC Monitoring: Architecture and Interoperability Guidelines";

    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public InteroperabilityRecordManager(ProviderService providerService, IdCreator idCreator,
                                         SecurityService securityService, VocabularyService vocabularyService,
                                         ProviderResourcesCommonMethods commonMethods,
                                         GenericResourceService genericResourceService) {
        super(genericResourceService, securityService, vocabularyService);
        this.providerService = providerService;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "interoperability_record";
    }

    //region generic
    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle guideline, Authentication auth) {
        ProviderBundle provider = providerService.get((String) guideline.getInteroperabilityRecord().get("owner"),
                guideline.getCatalogueId());
        onboard(guideline, provider, auth);
        blockNamingAsEOSCMonitoringGuideline((String) guideline.getInteroperabilityRecord().get("name"));
        InteroperabilityRecordBundle ret = genericResourceService.add(getResourceTypeName(), guideline);
        return ret;
    }

    private void onboard(InteroperabilityRecordBundle guideline, ProviderBundle provider, Authentication auth) {
        String catalogueId = guideline.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            if (provider.getStatus().equals("approved")) {
                guideline.markOnboard(vocabularyService.get("pending").getId(), false, UserInfo.of(auth), null);
                guideline.setActive(true);
            } else {
                throw new ResourceException(String.format("The Provider '%s' you provided as a Owner " +
                        "is not yet approved", provider.getId()), HttpStatus.CONFLICT);
            }
            guideline.setCatalogueId(this.catalogueId);
            this.createIdentifiers(guideline, getResourceTypeName(), false);
            guideline.setId(guideline.getIdentifiers().getOriginalId());
        } else {
            guideline.markOnboard(vocabularyService.get("approved").getId(), true, UserInfo.of(auth), null);
//            commonMethods.validateCatalogueId(catalogueId); //FIXME
            idCreator.validateId(guideline.getId());
            this.createIdentifiers(guideline, getResourceTypeName(), true);
        }
        guideline.setAuditState(Auditable.NOT_AUDITED);
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle guideline, String comment, Authentication auth) {
        InteroperabilityRecordBundle existing = get(guideline.getId(), guideline.getCatalogueId());
        // check if there are actual changes in the Service
        if (guideline.equals(existing)) {
            return guideline;
        }
        guideline.markUpdate(UserInfo.of(auth), comment);

        blockNamingAsEOSCMonitoringGuideline((String) guideline.getInteroperabilityRecord().get("name"));
        try {
            return genericResourceService.update(getResourceTypeName(), guideline.getId(), guideline);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(InteroperabilityRecordBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Interoperability Record: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public InteroperabilityRecordBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        InteroperabilityRecordBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        logger.info("Verifying Interoperability Record: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InteroperabilityRecordBundle setActive(String id, Boolean active, Authentication auth) {
        InteroperabilityRecordBundle existing = get(id);

        ProviderBundle provider = providerService.get((String) existing.getInteroperabilityRecord().get("owner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Interoperability Record, as its Provider is inactive",
                    HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Interoperability Record, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    //endregion


    //region EOSC Resource-specific
    @Override
    public Paging<InteroperabilityRecordBundle> getAllEOSCResourcesOfAProvider(String providerId, String catalogueId,
                                                                               int quantity, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.setQuantity(quantity);
        ff.addOrderBy("name", "asc");
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        InteroperabilityRecordBundle guideline = get(id);
        ProviderBundle provider = providerService.get((String) guideline.getInteroperabilityRecord().get("owner"),
                guideline.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Interoperability Records", provider.getId());
//        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(service, provider); //FIXME
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    //FIXME
    @Override
    public List<InteroperabilityRecordBundle> getByIds(Authentication auth, String... ids) {
        List<InteroperabilityRecordBundle> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return resources;
    }

    //FIXME: find a better way to get EOSC Monitoring IG - name is not unique
    public InteroperabilityRecordBundle getEOSCMonitoringGuideline() {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.addFilter("name", "EOSC Monitoring: Architecture and Interoperability Guidelines");
        List<InteroperabilityRecordBundle> igList = getAll(ff).getResults();
        if (!igList.isEmpty()) {
            return igList.getFirst();
        }
        throw new CatalogueResourceNotFoundException("Could not find EOSC Monitoring Guideline");
    }

    private void blockNamingAsEOSCMonitoringGuideline(String name) {
        if (RESERVED_NAME.equals(name)) {
            throw new ValidationException(
                    String.format("Name '%s' is committed for the EOSC Monitoring Guideline", name)
            );
        }
    }
    //endregion

    //region Drafts
    @Override
    public InteroperabilityRecordBundle addDraft(InteroperabilityRecordBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        InteroperabilityRecordBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public InteroperabilityRecordBundle updateDraft(InteroperabilityRecordBundle bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            InteroperabilityRecordBundle ret = genericResourceService.update(getResourceTypeName(),
                    bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(InteroperabilityRecordBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public InteroperabilityRecordBundle finalizeDraft(InteroperabilityRecordBundle guideline, Authentication auth) {
        ProviderBundle provider = providerService.get((String) guideline.getInteroperabilityRecord().get("owner"),
                guideline.getCatalogueId());
        UserInfo user = UserInfo.of(auth);
        if (provider.getTemplateStatus().equals("approved template")) {
            guideline.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
        } else {
            guideline.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
        }
        guideline = update(guideline, auth);

        return guideline;
    }
    //endregion
}
