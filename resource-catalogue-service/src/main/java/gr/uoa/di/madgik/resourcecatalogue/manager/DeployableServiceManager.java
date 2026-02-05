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
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service("deployableServiceManager")
public class DeployableServiceManager extends ResourceCatalogueGenericManager<DeployableServiceBundle>
        implements DeployableServiceService {

    private static final Logger logger = LoggerFactory.getLogger(DeployableServiceManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public DeployableServiceManager(ProviderService providerService,
                                    IdCreator idCreator, @Lazy SecurityService securityService,
                                    @Lazy VocabularyService vocabularyService,
                                    @Lazy ProviderResourcesCommonMethods commonMethods,
                                    GenericResourceService genericResourceService) {
        super(genericResourceService, securityService, vocabularyService);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "deployable_service";
    }

    //region generic
    @Override
    public DeployableServiceBundle add(DeployableServiceBundle deployableService, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("owner"),
                deployableService.getCatalogueId());
        onboard(deployableService, provider, auth);
        onboardingValidation(provider);
        DeployableServiceBundle ret = genericResourceService.add(getResourceTypeName(), deployableService);
        return ret;
    }

    private void onboard(DeployableServiceBundle deployableService, ProviderBundle provider, Authentication auth) {
        String catalogueId = deployableService.getCatalogueId();
        UserInfo user = UserInfo.of(auth);
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            if (provider.getTemplateStatus().equals("approved template")) {
                deployableService.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
                deployableService.setActive(true);
            } else {
                deployableService.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
            }
            deployableService.setCatalogueId(this.catalogueId);
            this.createIdentifiers(deployableService, getResourceTypeName(), false);
            deployableService.setId(deployableService.getIdentifiers().getOriginalId());
        } else {
            deployableService.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
//            commonMethods.validateCatalogueId(catalogueId); //FIXME
            idCreator.validateId(deployableService.getId());
            this.createIdentifiers(deployableService, getResourceTypeName(), true);
        }
        deployableService.setAuditState(Auditable.NOT_AUDITED);
    }

    private void onboardingValidation(ProviderBundle provider) {
        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
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
    public DeployableServiceBundle update(DeployableServiceBundle deployableService, String comment, Authentication auth) {
        DeployableServiceBundle existing = get(deployableService.getId(), deployableService.getCatalogueId());
        // check if there are actual changes in the Service
        if (deployableService.equals(existing)) {
            return deployableService;
        }
        deployableService.markUpdate(UserInfo.of(auth), comment);
        checkAndResetDeployableServiceOnboarding(deployableService, auth);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();

        try {
            return genericResourceService.update(getResourceTypeName(), deployableService.getId(), deployableService);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetDeployableServiceOnboarding(DeployableServiceBundle deployableService, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("owner"),
                deployableService.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (deployableService.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                deployableService.setStatus(vocabularyService.get("pending").getId());
                deployableService.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    public void delete(DeployableServiceBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Deployable Service: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public DeployableServiceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        DeployableServiceBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Deployable Service: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(DeployableServiceBundle deployableService, String status, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("owner"),
                deployableService.getCatalogueId());
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
    public DeployableServiceBundle setActive(String id, Boolean active, Authentication auth) {
        DeployableServiceBundle existing = get(id);

        ProviderBundle provider = providerService.get((String) existing.getDeployableService().get("owner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Deployable Service, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Deployable Service, because it is not yet approved.");
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
    public Paging<DeployableServiceBundle> getAllEOSCResourcesOfAProvider(String providerId, String catalogueId,
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
        DeployableServiceBundle deployableService = get(id);
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("owner"),
                deployableService.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Services", provider.getId());
//        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(service, provider); //FIXME
    }

    @Override
    public Browsing<DeployableServiceBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    //FIXME
    @Override
    public List<DeployableServiceBundle> getByIds(Authentication auth, String... ids) {
        List<DeployableServiceBundle> resources;
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
        List<DeployableServiceBundle> allProviderDeployableServices = getAll(ff, auth).getResults();
        for (DeployableServiceBundle bundle : allProviderDeployableServices) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }
    //endregion

    //region Drafts
    @Override
    public DeployableServiceBundle addDraft(DeployableServiceBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        DeployableServiceBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public DeployableServiceBundle updateDraft(DeployableServiceBundle bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            DeployableServiceBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(DeployableServiceBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public DeployableServiceBundle finalizeDraft(DeployableServiceBundle deployableService, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("owner"),
                deployableService.getCatalogueId());
        UserInfo user = UserInfo.of(auth);
        if (provider.getTemplateStatus().equals("approved template")) {
            deployableService.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
        } else {
            deployableService.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
        }
        deployableService = update(deployableService, auth);

        return deployableService;
    }
    //endregion
}
