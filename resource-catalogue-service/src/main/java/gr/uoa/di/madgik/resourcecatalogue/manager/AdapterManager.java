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
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service("adapterManager")
public class AdapterManager extends ResourceCatalogueGenericManager<AdapterBundle> implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManager.class);
    private final GenericResourceService genericResourceService;
    private final OrganisationService organisationService;
    private final EmailService emailService;

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public AdapterManager(OIDCSecurityService securityService,
                          VocabularyService vocabularyService,
                          IdCreator idCreator,
                          GenericResourceService genericResourceService,
                          OrganisationService organisationService,
                          EmailService emailService,
                          WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.genericResourceService = genericResourceService;
        this.organisationService = organisationService;
        this.emailService = emailService;
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

        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

        logger.info("Verifying Adapter: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        AdapterBundle adapter = get(id);
        OrganisationBundle provider = organisationService.get((String) adapter.getAdapter().get("resourceOwner"),
                adapter.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Adapters", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(adapter, provider);
    }

    @Override
    public Browsing<AdapterBundle> getMy(FacetFilter filter, Authentication auth) {
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
