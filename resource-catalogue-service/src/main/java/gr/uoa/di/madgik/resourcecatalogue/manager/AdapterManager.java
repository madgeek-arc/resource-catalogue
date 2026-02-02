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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@org.springframework.stereotype.Service("adapterManager")
public class AdapterManager extends ResourceCatalogueGenericManager<AdapterBundle> implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(AdapterManager.class);
    private final OIDCSecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;
    private final ServiceService serviceService;
    private final ProviderService providerService;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public AdapterManager(OIDCSecurityService securityService,
                          VocabularyService vocabularyService,
                          ProviderResourcesCommonMethods commonMethods,
                          IdCreator idCreator, ServiceService serviceService,
                          ProviderService providerService,
                          GenericResourceService genericResourceService) {
        super(genericResourceService, securityService);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
        this.serviceService = serviceService;
        this.providerService = providerService;
        this.genericResourceService = genericResourceService;
    }

    @Override
    protected String getResourceTypeName() {
        return "adapter";
    }

    @Override
    public AdapterBundle add(AdapterBundle bundle, Authentication auth) {
        onboard(bundle, auth);
        AdapterBundle ret = genericResourceService.add(getResourceTypeName(), bundle);
        return ret;
    }

    //TODO: revisit when we support external catalogue adapters
    private void onboard(AdapterBundle bundle, Authentication auth) {
        bundle.setCatalogueId(this.catalogueId);
        determineOnboard(bundle, auth);
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());
        commonMethods.addAuthenticatedUser(bundle.getAdapter(), auth);
        bundle.setAuditState(Auditable.NOT_AUDITED);
    }

    @Override
    public AdapterBundle update(AdapterBundle bundle, String comment, Authentication auth) {
        AdapterBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Provider
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(auth, comment);

        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(AdapterBundle bundle) {
        // block Public Provider deletion
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Adapter");
        }
        logger.info("Deleting ADapter: {} and all its Resources", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public AdapterBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        AdapterBundle existing = get(id);
        existing.markOnboard(status, active, auth, null);

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

        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Adapter, because it is not yet approved.");
        }

        existing.markActive(active, auth);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AdapterBundle setSuspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        AdapterBundle bundle = get(id, catalogueId);

        logger.info("Suspending Adapter: {} and all its Resources", bundle.getId());
        bundle.markSuspend(suspend, auth);

        try {
            return genericResourceService.update(getResourceTypeName(), id, bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Browsing<AdapterBundle> getMy(FacetFilter ff, Authentication auth) {
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }
    //endregion

    //region Adapter-specific
    @Override
    public boolean hasAdminAcceptedTerms(String id, Authentication auth) {
        AdapterBundle bundle = get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
        String userEmail = AuthenticationInfo.getEmail(auth).toLowerCase();

        List<String> adapterAdmins = extractEmails(bundle);
        List<String> acceptedTerms = bundle.getMetadata().getTerms();

        if (acceptedTerms == null || acceptedTerms.isEmpty()) {
            return !adapterAdmins.contains(userEmail); // false -> show modal, true -> no modal
        }

        return !adapterAdmins.contains(userEmail) || acceptedTerms.contains(userEmail); // Show or not modal
    }

    private List<String> extractEmails(AdapterBundle bundle) {
        List<String> emails = new ArrayList<>();

        Object usersObj = bundle.getAdapter().get("users");
        if (usersObj instanceof Collection<?>) {
            for (Object obj : (Collection<?>) usersObj) {
                if (obj instanceof User user) {
                    emails.add(user.getEmail().toLowerCase());
                }
            }
        }
        return emails;
    }

    @Override
    public void adminAcceptedTerms(String id, Authentication auth) {
        AdapterBundle bundle = get(id);
        String userEmail = AuthenticationInfo.getEmail(auth);

        List<String> existingTerms = bundle.getMetadata().getTerms();
        if (existingTerms == null) {
            existingTerms = new ArrayList<>();
        }

        if (!existingTerms.contains(userEmail)) {
            existingTerms.add(userEmail);
            bundle.getMetadata().setTerms(existingTerms);

            try {
                genericResourceService.update(getResourceTypeName(), id, bundle);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.info("Could not update terms for Adapter with id: '{}'", id);
            } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void determineOnboard(AdapterBundle bundle, Authentication auth) {
        if (securityService.hasPortalAdminRole(auth) || securityService.hasRole(auth, "ROLE_PROVIDER")) {
            bundle.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
        } else if (securityService.hasRole(auth, "ROLE_USER")) {
            bundle.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
        } else {
            throw new AccessDeniedException("You do not have permission to perform this action");
        }
    }
    //endregion

    //region Drafts
    @Override
    public AdapterBundle addDraft(AdapterBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());
        commonMethods.addAuthenticatedUser(bundle.getAdapter(), auth);

        AdapterBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public AdapterBundle updateDraft(AdapterBundle bundle, Authentication auth) {
        bundle.markUpdate(auth, null);
        try {
            AdapterBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(AdapterBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public AdapterBundle finalizeDraft(AdapterBundle bundle, Authentication auth) {
        determineOnboard(bundle, auth);
        bundle = update(bundle, auth);
        return bundle;
    }
    //endregion
}
