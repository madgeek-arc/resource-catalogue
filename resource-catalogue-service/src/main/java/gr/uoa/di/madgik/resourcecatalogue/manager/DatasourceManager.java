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
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.TriggersAspects;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
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

    private final ProviderService providerService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final OpenAIREDatasourceManager openAIREDatasourceManager;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final GenericResourceService genericResourceService;
    private final RelationshipValidator relationshipValidator;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public DatasourceManager(ProviderService providerService,
                             @Lazy VocabularyService vocabularyService,
                             @Lazy ProviderResourcesCommonMethods commonMethods,
                             OpenAIREDatasourceManager openAIREDatasourceManager,
                             IdCreator idCreator,
                             GenericResourceService genericResourceService,
                             SecurityService securityService,
                             RelationshipValidator relationshipValidator) {
        super(genericResourceService, securityService);
        this.providerService = providerService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.openAIREDatasourceManager = openAIREDatasourceManager;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.genericResourceService = genericResourceService;
        this.relationshipValidator = relationshipValidator;
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }

    //region generic
    @Override
    public DatasourceBundle add(DatasourceBundle datasource, Authentication auth) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("owner"),
                datasource.getCatalogueId());
        onboard(datasource, provider, auth);
        onboardingValidation(datasource, provider);
        DatasourceBundle ret = genericResourceService.add(getResourceTypeName(), datasource);
        return ret;
    }

    private void onboard(DatasourceBundle datasource, ProviderBundle provider, Authentication auth) {
        String catalogueId = datasource.getCatalogueId();
        UserInfo user = UserInfo.of(auth);
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            if (provider.getTemplateStatus().equals("approved template")) {
                datasource.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
                datasource.setActive(true);
            } else {
                datasource.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
            }
            datasource.setCatalogueId(this.catalogueId);
            this.createIdentifiers(datasource, getResourceTypeName(), false);
            datasource.setId(datasource.getIdentifiers().getOriginalId());
        } else {
            datasource.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
//            commonMethods.validateCatalogueId(catalogueId); //FIXME
            idCreator.validateId(datasource.getId());
            this.createIdentifiers(datasource, getResourceTypeName(), true);
        }
        datasource.setAuditState(Auditable.NOT_AUDITED);
    }

    private void onboardingValidation(DatasourceBundle datasource, ProviderBundle provider) {
        relationshipValidator.checkRelatedResourceIDsConsistency(datasource);
        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();
        if (!provider.getStatus().equals("approved")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Owner " +
                    "is not yet approved", provider.getId()), HttpStatus.CONFLICT);
        }
        if (provider.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource " +
                    "Template.", provider.getId()), HttpStatus.CONFLICT);
        }
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

        try {
            return genericResourceService.update(getResourceTypeName(), datasource.getId(), datasource);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetDatasourceOnboarding(DatasourceBundle datasource, Authentication auth) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("owner"),
                datasource.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (datasource.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                datasource.setStatus(vocabularyService.get("pending").getId());
                datasource.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    @Transactional
    public void delete(DatasourceBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        commonMethods.deleteResourceInteroperabilityRecords(bundle.getId(), getResourceTypeName());
        logger.info("Deleting Datasource: {} and all its Resource Interoperability Records", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public DatasourceBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        DatasourceBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Datasource: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(DatasourceBundle datasource, String status, Authentication auth) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("owner"),
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
        providerService.update(provider, "system update", auth);
    }

    @Override
    public DatasourceBundle setActive(String id, Boolean active, Authentication auth) {
        DatasourceBundle existing = get(id);

        ProviderBundle provider = providerService.get((String) existing.getDatasource().get("owner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Datasource, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Datasource, because it is not yet approved.");
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
    public Paging<DatasourceBundle> getAllEOSCResourcesOfAProvider(String providerId, String catalogueId,
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
        DatasourceBundle datasource = get(id);
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("owner"),
                datasource.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Services", provider.getId());
//        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(service, provider); //FIXME
    }

    @Override
    public Browsing<DatasourceBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", false); // A Draft Provider cannot have resources
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        if (providers.isEmpty()) {
            return new Browsing<>();
        }

        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(maxQuantity);
        filter.addFilter("published", false);
        filter.addFilter("owner", providers.stream().map(ProviderBundle::getId).toList());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    //FIXME
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
        ff.addFilter("owner", providerId);
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
            datasourceBundle.setOriginalOpenAIREId(datasourceBundle.getId());
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

    //region Drafts
    @Override
    public DatasourceBundle addDraft(DatasourceBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        DatasourceBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public DatasourceBundle updateDraft(DatasourceBundle bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            DatasourceBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(DatasourceBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public DatasourceBundle finalizeDraft(DatasourceBundle datasource, Authentication auth) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("owner"),
                datasource.getCatalogueId());
        UserInfo user = UserInfo.of(auth);
        if (provider.getTemplateStatus().equals("approved template")) {
            datasource.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
        } else {
            datasource.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
        }
        datasource = update(datasource, auth);

        return datasource;
    }
    //endregion
}