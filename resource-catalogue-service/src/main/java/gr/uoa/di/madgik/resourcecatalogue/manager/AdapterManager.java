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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingTemplateHandler;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.TemplateOnboardingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@org.springframework.stereotype.Service("adapterManager")
public class AdapterManager extends ResourceCatalogueGenericManager<AdapterBundle> implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManager.class);
    private final GenericResourceService genericResourceService;
    private final OrganisationService organisationService;
    private final EmailService emailService;
    private final OnboardingTemplateHandler templateHandler;

    public AdapterManager(OIDCSecurityService securityService,
                          VocabularyService vocabularyService,
                          IdCreator idCreator,
                          GenericResourceService genericResourceService,
                          OrganisationService organisationService,
                          EmailService emailService,
                          WorkflowService workflowService,
                          OnboardingTemplateHandler templateHandler) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.genericResourceService = genericResourceService;
        this.organisationService = organisationService;
        this.emailService = emailService;
        this.templateHandler = templateHandler;
    }

    @Override
    protected String getResourceTypeName() {
        return "adapter";
    }

    //region generic
    @Override
    public AdapterBundle update(AdapterBundle bundle, String comment, Authentication auth) {
        AdapterBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Provider
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(UserInfo.of(auth), comment);
        templateHandler.resetRejectedOnboarding(getResourceTypeName(), bundle, auth);
        discardStaleSqaResult(existing, bundle);

        return genericResourceService.update(getResourceTypeName(), bundle);
    }

    /**
     * The SQA result describes the repository that was assessed. When the repository changes, the result is
     * dropped, so the SQA aspect (which skips adapters that already have an SQA URL) assesses the new one.
     * A result the caller changed on purpose is left alone.
     */
    @SuppressWarnings("unchecked")
    private void discardStaleSqaResult(AdapterBundle existing, AdapterBundle updated) {
        Object oldRepository = existing.getAdapter().get("repository");
        Object newRepository = updated.getAdapter().get("repository");
        Object sqa = updated.getAdapter().get("sqa");
        if (Objects.equals(oldRepository, newRepository)
                || !Objects.equals(existing.getAdapter().get("sqa"), sqa)
                || !(sqa instanceof Map<?, ?> sqaMap)) {
            return;
        }
        ((Map<String, Object>) sqaMap).remove("sqaURL");
        ((Map<String, Object>) sqaMap).remove("sqaBadge");
        if (sqaMap.isEmpty()) {
            updated.getAdapter().remove("sqa"); // don't leave an empty "sqa": {} behind
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public AdapterBundle updateSqa(String id, String catalogueId, String assessedRepository, String sqaUrl,
                                   String sqaBadge) {
        AdapterBundle stored = get(id, catalogueId);
        if (!Objects.equals(stored.getAdapter().get("repository"), assessedRepository)) {
            return null;
        }
        Object current = stored.getAdapter().get("sqa");
        Map<String, Object> sqa = current instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
        sqa.put("sqaURL", sqaUrl);
        if (sqaBadge == null) {
            sqa.remove("sqaBadge"); // the new assessment earned no badge, so an older one no longer applies
        } else {
            sqa.put("sqaBadge", sqaBadge);
        }
        stored.getAdapter().put("sqa", sqa);
        return genericResourceService.update(getResourceTypeName(), stored);
    }

    @Override
    public void delete(AdapterBundle bundle) {
        blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Adapter: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public AdapterBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        AdapterBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        templateHandler.syncTemplateStatus(getResourceTypeName(), existing, status, auth);

        logger.info("Verifying Adapter: {}", existing);
        return genericResourceService.update(getResourceTypeName(), existing);
    }

    @Override
    public AdapterBundle setActive(String id, Boolean active, Authentication auth) {
        AdapterBundle existing = get(id);

        OrganisationBundle provider = organisationService.get((String) existing.getAdapter().get("resourceOwner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Adapter, as its Provider is inactive",
                    HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Adapter, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        return genericResourceService.update(getResourceTypeName(), existing);
    }

    @Override
    public AdapterBundle changeResourceOwner(String id, String newOwnerId, String comment, Authentication auth) {
        AdapterBundle existing = get(id);
        if (existing.isDraft()) {
            throw new ValidationException(String.format("Adapter [%s] is a draft; drafts cannot be moved between "
                    + "Organisations.", id));
        }
        if (existing.isSuspended()) {
            throw new ResourceException("You cannot change the owner of a suspended Adapter.", HttpStatus.CONFLICT);
        }

        String currentOwnerId = (String) existing.getAdapter().get("resourceOwner");
        if (Objects.equals(currentOwnerId, newOwnerId)) {
            throw new ValidationException(String.format("Adapter [%s] is already owned by Organisation [%s].",
                    id, newOwnerId));
        }

        OrganisationBundle newOwner = organisationService.get(newOwnerId, existing.getCatalogueId());
        if (!newOwner.getStatus().equals(vocabularyService.get("approved").getId())) {
            throw new ValidationException(String.format(
                    "You cannot move Adapter [%s] to Organisation [%s], because it is not approved.", id, newOwnerId));
        }
        if (!newOwner.isActive()) {
            throw new ResourceException(String.format(
                    "You cannot move Adapter [%s] to Organisation [%s], because it is inactive.", id, newOwnerId),
                    HttpStatus.CONFLICT);
        }
        if (newOwner.isSuspended()) {
            throw new ResourceException(String.format(
                    "You cannot move Adapter [%s] to Organisation [%s], because it is suspended.", id, newOwnerId),
                    HttpStatus.CONFLICT);
        }

        logger.info("Changing owner of Adapter [{}] from Organisation [{}] to [{}]", id, currentOwnerId, newOwnerId);
        existing.getAdapter().put("resourceOwner", newOwnerId);
        existing.markUpdate(UserInfo.of(auth), comment, LoggingInfo.ActionType.MOVED);

        return genericResourceService.update(getResourceTypeName(), existing);
    }
    //endregion

    //region EOSC Resource-specific
    @Override
    public Paging<AdapterBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    @Override
    public Bundle getTemplate(String providerId, Authentication auth) {
        if (!templateHandler.isReviewFirst(getResourceTypeName())) {
            return null;
        }
        return TemplateOnboardingUtils.getTemplate(providerId, auth, this, vocabularyService);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        AdapterBundle adapter = get(id);
        OrganisationBundle provider = organisationService.get((String) adapter.getAdapter().get("resourceOwner"),
                adapter.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Adapters", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(adapter, provider);
    }

    @Override
    public Paging<AdapterBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    @Override
    public List<AdapterBundle> getByIds(Authentication auth, String... ids) {
        List<AdapterBundle> resources;
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
    //endregion
}
