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
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableApplicationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
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

@org.springframework.stereotype.Service("deployableApplicationManager")
public class DeployableApplicationManager extends ResourceCatalogueGenericManager<DeployableApplicationBundle>
        implements DeployableApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DeployableApplicationManager.class);

    private final OrganisationService organisationService;
    private final GenericResourceService genericResourceService;
    private final EmailService emailService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public DeployableApplicationManager(OrganisationService organisationService,
                                        IdCreator idCreator,
                                        @Lazy SecurityService securityService,
                                        @Lazy VocabularyService vocabularyService,
                                        GenericResourceService genericResourceService,
                                        EmailService emailService,
                                        WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.organisationService = organisationService;
        this.genericResourceService = genericResourceService;
        this.emailService = emailService;
    }

    @Override
    public String getResourceTypeName() {
        return "deployable_application";
    }

    //region generic
    @Override
    @Transactional
    public DeployableApplicationBundle update(DeployableApplicationBundle deployableApplication, String comment, Authentication auth) {
        DeployableApplicationBundle existing = get(deployableApplication.getId(), deployableApplication.getCatalogueId());
        // check if there are actual changes in the Service
        if (deployableApplication.equals(existing)) {
            return deployableApplication;
        }
        deployableApplication.markUpdate(UserInfo.of(auth), comment);
        checkAndResetDeployableApplicationOnboarding(deployableApplication, auth);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();

        try {
            return genericResourceService.update(getResourceTypeName(), deployableApplication.getId(), deployableApplication);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetDeployableApplicationOnboarding(DeployableApplicationBundle deployableApplication, Authentication auth) {
        OrganisationBundle provider = organisationService.get((String) deployableApplication.getDeployableApplication().get("resourceOwner"),
                deployableApplication.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (deployableApplication.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                deployableApplication.setStatus(vocabularyService.get("pending").getId());
                deployableApplication.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                organisationService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    public void delete(DeployableApplicationBundle bundle) {
        blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Deployable Application: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public DeployableApplicationBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        DeployableApplicationBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Deployable Application: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(DeployableApplicationBundle deployableApplication, String status, Authentication auth) {
        OrganisationBundle provider = organisationService.get((String) deployableApplication.getDeployableApplication().get("resourceOwner"),
                deployableApplication.getCatalogueId());
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
    public DeployableApplicationBundle setActive(String id, Boolean active, Authentication auth) {
        DeployableApplicationBundle existing = get(id);

        OrganisationBundle provider = organisationService.get((String) existing.getDeployableApplication().get("resourceOwner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Deployable Application, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Deployable Application, because it is not yet approved.");
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
    public Paging<DeployableApplicationBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        DeployableApplicationBundle deployableApplication = get(id);
        OrganisationBundle provider = organisationService.get((String) deployableApplication.getDeployableApplication().get("resourceOwner"),
                deployableApplication.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Services", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(deployableApplication, provider);
    }

    @Override
    public Browsing<DeployableApplicationBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    @Override
    public List<DeployableApplicationBundle> getByIds(Authentication auth, String... ids) {
        List<DeployableApplicationBundle> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id);
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
        List<DeployableApplicationBundle> allProviderDeployableApplication = getAll(ff, auth).getResults();
        for (DeployableApplicationBundle bundle : allProviderDeployableApplication) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }
    //endregion
}
