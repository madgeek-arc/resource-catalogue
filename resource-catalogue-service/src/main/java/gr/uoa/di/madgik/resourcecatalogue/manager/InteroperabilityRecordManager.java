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
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
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

    private final OrganisationService organisationService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;
    private final EmailService emailService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public InteroperabilityRecordManager(OrganisationService organisationService, IdCreator idCreator,
                                         SecurityService securityService, VocabularyService vocabularyService,
                                         ProviderResourcesCommonMethods commonMethods,
                                         GenericResourceService genericResourceService,
                                         EmailService emailService,
                                         WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.organisationService = organisationService;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
        this.emailService = emailService;
    }

    @Override
    public String getResourceTypeName() {
        return "interoperability_record";
    }

    //region generic
    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle guideline, String comment, Authentication auth) {
        InteroperabilityRecordBundle existing = get(guideline.getId(), guideline.getCatalogueId());
        // check if there are actual changes in the Service
        if (guideline.equals(existing)) {
            return guideline;
        }
        guideline.markUpdate(UserInfo.of(auth), comment);

        blockNamingAsEOSCMonitoringGuideline((String) guideline.getInteroperabilityRecord().get("name"),
                (String) existing.getInteroperabilityRecord().get("name"));
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

        OrganisationBundle provider = organisationService.get((String) existing.getInteroperabilityRecord().get("resourceOwner"),
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
    public Paging<InteroperabilityRecordBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        InteroperabilityRecordBundle guideline = get(id);
        OrganisationBundle provider = organisationService.get((String) guideline.getInteroperabilityRecord().get("resourceOwner"),
                guideline.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Interoperability Records", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(guideline, provider);
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

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

    private void blockNamingAsEOSCMonitoringGuideline(String name, String existingName) {
        if (!RESERVED_NAME.equals(existingName) && RESERVED_NAME.equals(name)) {
            throw new ValidationException(
                    String.format("Name '%s' is committed for the EOSC Monitoring Guideline", name)
            );
        }
    }
    //endregion
}
