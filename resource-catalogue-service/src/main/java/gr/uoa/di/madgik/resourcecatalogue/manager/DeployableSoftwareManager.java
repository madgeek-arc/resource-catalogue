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
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
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

@org.springframework.stereotype.Service("deployableSoftwareManager")
public class DeployableSoftwareManager extends ResourceCatalogueGenericManager<DeployableSoftwareBundle>
        implements DeployableSoftwareService {

    private static final Logger logger = LoggerFactory.getLogger(DeployableSoftwareManager.class);

    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;
    private final WorkflowService workflowService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public DeployableSoftwareManager(ProviderService providerService,
                                     IdCreator idCreator,
                                     @Lazy SecurityService securityService,
                                     @Lazy VocabularyService vocabularyService,
                                     @Lazy ProviderResourcesCommonMethods commonMethods,
                                     GenericResourceService genericResourceService,
                                     WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService);
        this.providerService = providerService;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
        this.workflowService = workflowService;
    }

    @Override
    public String getResourceTypeName() {
        return "deployable_software";
    }

    //region generic
    @Override
    @Transactional
    public DeployableSoftwareBundle update(DeployableSoftwareBundle deployableSoftware, String comment, Authentication auth) {
        DeployableSoftwareBundle existing = get(deployableSoftware.getId(), deployableSoftware.getCatalogueId());
        // check if there are actual changes in the Service
        if (deployableSoftware.equals(existing)) {
            return deployableSoftware;
        }
        deployableSoftware.markUpdate(UserInfo.of(auth), comment);
        checkAndResetDeployableSoftwareOnboarding(deployableSoftware, auth);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();

        try {
            return genericResourceService.update(getResourceTypeName(), deployableSoftware.getId(), deployableSoftware);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetDeployableSoftwareOnboarding(DeployableSoftwareBundle deployableSoftware, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableSoftware.getDeployableSoftware().get("resourceOwner"),
                deployableSoftware.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (deployableSoftware.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                deployableSoftware.setStatus(vocabularyService.get("pending").getId());
                deployableSoftware.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    public void delete(DeployableSoftwareBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Deployable Software: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public DeployableSoftwareBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        DeployableSoftwareBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Deployable Software: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(DeployableSoftwareBundle deployableSoftware, String status, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableSoftware.getDeployableSoftware().get("resourceOwner"),
                deployableSoftware.getCatalogueId());
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
    public DeployableSoftwareBundle setActive(String id, Boolean active, Authentication auth) {
        DeployableSoftwareBundle existing = get(id);

        ProviderBundle provider = providerService.get((String) existing.getDeployableSoftware().get("resourceOwner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Deployable Software, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Deployable Software, because it is not yet approved.");
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
    public Paging<DeployableSoftwareBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        DeployableSoftwareBundle deployableSoftware = get(id);
        ProviderBundle provider = providerService.get((String) deployableSoftware.getDeployableSoftware().get("resourceOwner"),
                deployableSoftware.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Services", provider.getId());
//        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(service, provider); //FIXME
    }

    @Override
    public Browsing<DeployableSoftwareBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    //FIXME
    @Override
    public List<DeployableSoftwareBundle> getByIds(Authentication auth, String... ids) {
        List<DeployableSoftwareBundle> resources;
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
        List<DeployableSoftwareBundle> allProviderDeployableSoftware = getAll(ff, auth).getResults();
        for (DeployableSoftwareBundle bundle : allProviderDeployableSoftware) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }
    //endregion

    //region Drafts
    @Override
    public DeployableSoftwareBundle addDraft(DeployableSoftwareBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        DeployableSoftwareBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public DeployableSoftwareBundle updateDraft(DeployableSoftwareBundle bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            DeployableSoftwareBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(DeployableSoftwareBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public DeployableSoftwareBundle finalizeDraft(DeployableSoftwareBundle deployableSoftware, Authentication auth) {
        ProviderBundle provider = providerService.get((String) deployableSoftware.getDeployableSoftware().get("resourceOwner"),
                deployableSoftware.getCatalogueId());
        UserInfo user = UserInfo.of(auth);
        if (provider.getTemplateStatus().equals("approved template")) {
            deployableSoftware.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
        } else {
            deployableSoftware.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
        }
        deployableSoftware = update(deployableSoftware, auth);

        return deployableSoftware;
    }
    //endregion
}
