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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueResources;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.CatalogueResourceAggregator;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service("catalogueManager")
public class CatalogueManager extends ResourceCatalogueGenericManager<CatalogueBundle> implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueManager.class);

    private final OrganisationService organisationService;
    private final GenericResourceService genericResourceService;
    private final RelationshipValidator relationshipValidator;
    private final ResourceInteroperabilityRecordService rirService;
    private final EmailService emailService;
    private final CatalogueResourceAggregator cascadeLifecycleManager;

    public CatalogueManager(OrganisationService organisationService,
                            IdCreator idCreator,
                            SecurityService securityService,
                            VocabularyService vocabularyService,
                            GenericResourceService genericResourceService,
                            @Lazy RelationshipValidator relationshipValidator,
                            EmailService emailService,
                            ResourceInteroperabilityRecordService rirService,
                            WorkflowService workflowService,
                            @Lazy CatalogueResourceAggregator cascadeLifecycleManager) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.organisationService = organisationService;
        this.genericResourceService = genericResourceService;
        this.relationshipValidator = relationshipValidator;
        this.rirService = rirService;
        this.emailService = emailService;
        this.cascadeLifecycleManager = cascadeLifecycleManager;
    }

    @Override
    protected String getResourceTypeName() {
        return "catalogue";
    }

    //region generic
    @Override
    @Transactional
//    @TriggersAspects({"AfterServiceUpdateEmails"})
    public CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth) {
        CatalogueBundle existing = get(catalogue.getId());
        // check if there are actual changes in the Catalogue
        if (catalogue.equals(existing)) {
            return catalogue;
        }
        catalogue.markUpdate(UserInfo.of(auth), comment);
        relationshipValidator.checkRelatedResourceIDsConsistency(catalogue);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();

        return genericResourceService.update(getResourceTypeName(), catalogue);
    }

    @Override
    @Transactional
    public void delete(CatalogueBundle bundle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // block Public Catalogue deletion
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Catalogue");
        }
        logger.info("Deleting Catalogue: {} and all its resources", bundle.getId());
        cascadeLifecycleManager.deleteAllCatalogueRelatedResources(bundle.getId(), auth);
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public CatalogueBundle setSuspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        CatalogueBundle bundle = get(id, catalogueId);
        if (bundle.getMetadata().isPublished()) {
            throw new ResourceException("You cannot directly suspend a Public Catalogue", HttpStatus.FORBIDDEN);
        }
        logger.info("{} Catalogue '{}' and all its resources", suspend ? "Suspending" : "Unsuspending", id);
        bundle.markSuspend(suspend, auth);
        cascadeLifecycleManager.suspendAllCatalogueRelatedResources(id, suspend, auth);
        return genericResourceService.update(getResourceTypeName(), bundle);
    }

    @Transactional
    public CatalogueBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        CatalogueBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        logger.info("Verifying Catalogue: {}", existing);
        return genericResourceService.update(getResourceTypeName(), existing);
    }

    @Override
    public CatalogueBundle setActive(String id, Boolean active, Authentication auth) {
        CatalogueBundle existing = get(id);

        OrganisationBundle provider = organisationService.get((String) existing.getCatalogue().get("resourceOwner"));
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Catalogue, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Catalogue, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        return genericResourceService.update(getResourceTypeName(), existing);
    }
    //endregion

    //region EOSC Resource-specific
    @Override
    public Paging<CatalogueBundle> getAllEOSCResourcesOfAProvider(String providerId, FacetFilter ff, Authentication auth) {
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        CatalogueBundle catalogue = get(id);
        OrganisationBundle provider = organisationService.get((String) catalogue.getCatalogue().get("resourceOwner"));
        logger.info("Sending email to Provider '{}' for outdated Catalogues", provider.getId());
        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(catalogue, provider);
    }

    @Override
    public Paging<CatalogueBundle> getMy(FacetFilter filter, Authentication auth) {
        return getMyResources(filter, auth);
    }

    @Override
    public List<CatalogueBundle> getByIds(Authentication auth, String... ids) {
        List<CatalogueBundle> resources;
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

    // region Catalogue-specific
    public CatalogueResources getAllCatalogueResources(String catalogueId) {
        return cascadeLifecycleManager.getAllCatalogueResources(catalogueId);
    }
    //endregion
}
