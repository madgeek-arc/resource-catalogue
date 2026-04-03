/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service
public class DatasourceManager extends ResourceCatalogueGenericManager<DatasourceBundle> implements DatasourceService {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceManager.class);

    private final OrganisationService organisationService;
    private final OpenAIREDatasourceManager openAIREDatasourceManager;
    private final GenericResourceService genericResourceService;
    private final RelationshipValidator relationshipValidator;
    private final ResourceInteroperabilityRecordService rirService;
    private final EmailService emailService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public DatasourceManager(OrganisationService organisationService,
                             @Lazy VocabularyService vocabularyService,
                             OpenAIREDatasourceManager openAIREDatasourceManager,
                             IdCreator idCreator,
                             GenericResourceService genericResourceService,
                             SecurityService securityService,
                             RelationshipValidator relationshipValidator,
                             ResourceInteroperabilityRecordService rirService,
                             EmailService emailService,
                             WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.organisationService = organisationService;
        this.openAIREDatasourceManager = openAIREDatasourceManager;
        this.genericResourceService = genericResourceService;
        this.relationshipValidator = relationshipValidator;
        this.rirService = rirService;
        this.emailService = emailService;
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }

    //region generic
    @Override
    public DatasourceBundle add(DatasourceBundle bundle, Authentication auth) {
        // if Datasource has ID -> check if it exists in OpenAIRE Datasource list
        Object raw = bundle.getDatasource() != null ? bundle.getDatasource().get("id") : null;
        String id = (String) raw;
        if (id != null && !id.isEmpty()) {
            checkOpenAIREIDExistence(bundle);
        }
        return super.add(bundle, auth);
    }

    @Override
    @Transactional
//    @TriggersAspects({"AfterServiceUpdateEmails"})
    public DatasourceBundle update(DatasourceBundle datasource, String comment, Authentication auth) {
        DatasourceBundle existing = get(datasource.getId(), datasource.getCatalogueId());
        // check if there are actual changes in the Service
        if (datasource.equals(existing)) {
            return datasource;
        }
        datasource.markUpdate(UserInfo.of(auth), comment);
        relationshipValidator.checkRelatedResourceIDsConsistency(datasource);
        checkAndResetDatasourceOnboarding(datasource, auth);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();

        return genericResourceService.update(getResourceTypeName(), datasource);
    }

    private void checkAndResetDatasourceOnboarding(DatasourceBundle datasource, Authentication auth) {
        OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (datasource.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                datasource.setStatus(vocabularyService.get("pending").getId());
                datasource.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                organisationService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    @Transactional
    public void delete(DatasourceBundle bundle) {
        blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        deleteResourceInteroperabilityRecords(bundle.getId(), getResourceTypeName());
        logger.info("Deleting Datasource: {} and all its Resource Interoperability Records", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public DatasourceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        DatasourceBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Datasource: {}", existing);
        return genericResourceService.update(getResourceTypeName(), existing);
    }

    private void updateProviderTemplateStatus(DatasourceBundle datasource, String status, Authentication auth) {
        OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        switch (status) {
            case "pending":
                provider.setTemplateStatus("pending template");
                break;
            case "approved":
                provider.setTemplateStatus("approved template");
                break;
            case "rejected":
                provider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        organisationService.update(provider, "system update", auth);
    }

    @Override
    public DatasourceBundle setActive(String id, Boolean active, Authentication auth) {
        DatasourceBundle existing = get(id);

        OrganisationBundle provider = organisationService.get((String) existing.getDatasource().get("resourceOwner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Datasource, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Datasource, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        return genericResourceService.update(getResourceTypeName(), existing);
    }
    //endregion

    //region EOSC Resource-specific
    @Override
    public Paging<DatasourceBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        DatasourceBundle datasource = get(id);
        OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Services", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(datasource, provider);
    }

    @Override
    public Paging<DatasourceBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    @Override
    public List<DatasourceBundle> getByIds(Authentication auth, String... ids) {
        List<DatasourceBundle> resources;
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

    @Override
    public Bundle getTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<DatasourceBundle> allProviderServices = getAll(ff, auth).getResults();
        for (DatasourceBundle bundle : allProviderServices) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }

    // OpenAIRE
    private void checkOpenAIREIDExistence(DatasourceBundle datasourceBundle) {
        LinkedHashMap<String, Object> datasource = openAIREDatasourceManager.get(datasourceBundle.getId());
        if (datasource != null) {
            datasourceBundle.setOriginalOpenAIREId(datasourceBundle.getId()); //TODO: create AlternativeIdentifiers inside Identifiers and move there?
        } else {
            throw new CatalogueResourceNotFoundException(String.format("The ID [%s] you provided does not belong to an " +
                    "OpenAIRE Datasource", datasourceBundle.getId()));
        }
    }

    public boolean isDatasourceRegisteredOnOpenAIRE(String id) {
        DatasourceBundle datasourceBundle = get(id);
        boolean found = false;
        String registerBy;
        if (datasourceBundle != null) {
            String originalOpenAIREId = datasourceBundle.getOriginalOpenAIREId();
            if (originalOpenAIREId != null && !originalOpenAIREId.isEmpty()) {
                registerBy = openAIREDatasourceManager.getRegisterBy(originalOpenAIREId);
                if (registerBy != null && !registerBy.isEmpty()) {
                    found = true;
                }
            }
        } else {
            throw new ResourceNotFoundException(id, "Datasource");
        }
        return found;
    }
    //endregion

    //region Service-specific
    private void deleteResourceInteroperabilityRecords(String resourceId, String resourceType) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = rirService.getByResourceId(resourceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                logger.info("Deleting ResourceInteroperabilityRecord of {} with id: '{}'", resourceType, resourceId);
                rirService.delete(resourceInteroperabilityRecordBundle);
            } catch (ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    //endregion
}