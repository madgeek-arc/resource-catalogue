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
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;

@Service("catalogueManager")
public class CatalogueManager extends ResourceCatalogueGenericManager<CatalogueBundle> implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueManager.class);
    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final ProviderResourcesCommonMethods commonMethods;

    private final GenericResourceService genericResourceService;
//    private final EmailService emailService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public CatalogueManager(IdCreator idCreator,
                            @Lazy ProviderService providerService,
                            @Lazy ServiceService serviceService,
                            @Lazy SecurityService securityService,
                            @Lazy VocabularyService vocabularyService,
                            @Lazy ProviderResourcesCommonMethods commonMethods,
//                            EmailService emailService,
                            GenericResourceService genericResourceService) {
        super(genericResourceService, securityService, vocabularyService);
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
//        this.emailService = emailService;
    }

    @Override
    public String getResourceTypeName() {
        return "catalogue";
    }

    @Override
    public CatalogueBundle get(String id) {
//        CatalogueBundle catalogue = super.get(id);
        CatalogueBundle catalogue = genericResourceService.get(getResourceTypeName(), id);
        //FIXME: never reaches here
        if (catalogue == null) {
            throw new ResourceNotFoundException(id, "Catalogue");
        }
        return catalogue;
    }

    @Override
    public CatalogueBundle get(String id, Authentication auth) {
        CatalogueBundle catalogueBundle = get(id);
        if (auth != null && auth.isAuthenticated()) {
            // if user is ADMIN/EPOT or Catalogue Admin on the specific Catalogue, return everything
            if (securityService.hasPortalAdminRole(auth) || securityService.hasAdminAccess(auth, id)) {
                return catalogueBundle;
            }
        }
        // else return the Catalogue ONLY if it is active
        if (catalogueBundle.getStatus().equals(vocabularyService.get("approved").getId())) {
            return catalogueBundle;
        }
        throw new InsufficientAuthenticationException("You cannot view the specific Catalogue");
    }

    @Override
    public Browsing<CatalogueBundle> getAll(FacetFilter ff, Authentication auth) {
        List<CatalogueBundle> userCatalogues = null;
        List<CatalogueBundle> retList = new ArrayList<>();

        // if user is ADMIN or EPOT return everything
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                    securityService.hasRole(auth, "ROLE_EPOT")) {
                return super.getAll(ff, auth);
            }

            Browsing<CatalogueBundle> catalogues = super.getAll(ff, auth);
            for (CatalogueBundle catalogueBundle : catalogues.getResults()) {
                if (catalogueBundle.getStatus().equals(vocabularyService.get("approved").getId()) ||
                        securityService.hasAdminAccess(auth, catalogueBundle.getId())) {
                    retList.add(catalogueBundle);
                }
            }
            catalogues.setResults(retList);
            catalogues.setTotal(retList.size());
            catalogues.setTo(retList.size());
            userCatalogues = getMy(null, auth).getResults();
            if (userCatalogues != null) {
                // replace user providers having null users with complete provider entries
                userCatalogues.forEach(x -> {
                    catalogues.getResults().removeIf(catalogue -> catalogue.getId().equals(x.getId()));
                    catalogues.getResults().add(x);
                });
            }
            return catalogues;
        }

        // else return ONLY approved Catalogues
        ff.addFilter("status", "approved");
        Browsing<CatalogueBundle> catalogues = super.getAll(ff, auth);
        retList.addAll(catalogues.getResults());
        catalogues.setResults(retList);

        return catalogues;
    }

    @Override
    public CatalogueBundle add(CatalogueBundle catalogue, Authentication auth) {

        logger.trace("Attempting to add a new Catalogue: {}", catalogue);
        commonMethods.addAuthenticatedUser(catalogue.getCatalogue(), auth);
        validate(catalogue);
        catalogue.setId(idCreator.generate(this.getResourceTypeName()));



        CatalogueBundle ret;
        ret = super.add(catalogue, null);
        logger.debug("Adding Catalogue: {}", catalogue);

//        emailService.sendEmailsToNewlyAddedCatalogueAdmins(catalogue, null);

        return ret;
    }

    public CatalogueBundle update(CatalogueBundle catalogueBundle, String comment, Authentication auth) {
        logger.trace("Attempting to update the Catalogue with id '{}'", catalogueBundle.getId());

        CatalogueBundle existingCatalogue = genericResourceService.get(getResourceTypeName(), catalogueBundle.getId());
        // check if there are actual changes in the Catalogue
        if (catalogueBundle.getCatalogue().equals(existingCatalogue.getCatalogue())) {
            return catalogueBundle;
        }

        catalogueBundle.markUpdate(UserInfo.of(auth), comment);

        logger.debug("Updating Catalogue: {}", catalogueBundle);

        // Send emails to newly added or deleted Admins
        adminDifferences(catalogueBundle, existingCatalogue);

        CatalogueBundle ret = super.update(catalogueBundle, null);

        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//                emailService.notifyPortalAdminsForInvalidCatalogueUpdate(ret);
            }
        }

        return ret;
    }

    @Override
    public CatalogueBundle validate(CatalogueBundle catalogue) {
        logger.debug("Validating Catalogue with id: '{}'", catalogue.getId());
        return super.validate(catalogue);
    }

    @Override
    public void delete(CatalogueBundle catalogueBundle) {
        String id = catalogueBundle.getId();

        // Block accidental deletion of main Catalogue
        if (id.equals(catalogueId)) {
            throw new ResourceException(String.format("You cannot delete [%s] Catalogue.", catalogueId),
                    HttpStatus.FORBIDDEN);
        }

        // Delete Catalogue along with all its related Resources
        logger.info("Deleting all Catalogue's Providers...");
        deleteCatalogueResources(id, providerService, securityService.getAdminAccess());

        logger.info("Deleting all Catalogue's Services...");
        deleteCatalogueResources(id, serviceService, securityService.getAdminAccess());

//        logger.info("Deleting all Catalogue's Training Resources...");
//        deleteCatalogueResources(id, trainingResourceService, securityService.getAdminAccess());
//
//        logger.info("Deleting all Catalogue's Interoperability Records...");
//        deleteCatalogueResources(id, interoperabilityRecordService, securityService.getAdminAccess());

        logger.info("Deleting Catalogue...");
        genericResourceService.delete(getResourceTypeName(), catalogueBundle.getId());
    }

    @Override
    public Browsing<CatalogueBundle> getMy(FacetFilter ff, Authentication auth) {
        if (auth == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }
        if (ff == null) {
            ff = new FacetFilter();
//            ff.setQuantity(maxQuantity);
        }
        ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth);
    }

    private <T, I extends ResourceCRUDService<T, Authentication>> void deleteCatalogueResources(String id, I service, Authentication auth) {
        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(maxQuantity);
        ff.addFilter("catalogue_id", id);
        // Get all Catalogue's Resources
        List<T> allResources = service.getAll(ff, auth).getResults();
        for (T resource : allResources) {
            try {
                logger.info("Deleting resource: {}", resource);
                service.delete(resource);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void adminDifferences(CatalogueBundle updatedCatalogue, CatalogueBundle existingCatalogue) {
        List<String> existingAdmins = new ArrayList<>();
        List<String> newAdmins = new ArrayList<>();
//        for (User user : existingCatalogue.getCatalogue().getUsers()) {
//            existingAdmins.add(user.getEmail().toLowerCase());
//        }
//        for (User user : updatedCatalogue.getCatalogue().getUsers()) {
//            newAdmins.add(user.getEmail().toLowerCase());
//        }
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        if (!adminsAdded.isEmpty()) {
//            emailService.sendEmailsToNewlyAddedCatalogueAdmins(updatedCatalogue, adminsAdded);
        }
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);
        if (!adminsDeleted.isEmpty()) {
//            emailService.sendEmailsToNewlyDeletedCatalogueAdmins(updatedCatalogue, adminsDeleted);
        }
    }

    @Override
    public boolean hasAdminAcceptedTerms(String id, Authentication auth) {
//        CatalogueBundle bundle = get(id);
//        String userEmail = AuthenticationInfo.getEmail(auth).toLowerCase();
//
//        List<String> catalogueAdmins = bundle.getCatalogue().getUsers().stream()
//                .map(user -> user.getEmail().toLowerCase())
//                .toList();
//
//        List<String> acceptedTerms = bundle.getMetadata().getTerms();
//
//        if (acceptedTerms == null || acceptedTerms.isEmpty()) {
//            return !catalogueAdmins.contains(userEmail); // false -> show modal, true -> no modal
//        }
//
//        if (catalogueAdmins.contains(userEmail) && !acceptedTerms.contains(userEmail)) {
//            return false; // Show modal
//        }
        return true; // No modal
    }

    @Override
    public void adminAcceptedTerms(String id, Authentication auth) {
        CatalogueBundle bundle = get(id);
        String userEmail = AuthenticationInfo.getEmail(auth);
        List<String> existingTerms = bundle.getMetadata().getTerms();
        if (existingTerms == null) {
            existingTerms = new ArrayList<>();
        }
        if (!existingTerms.contains(userEmail)) {
            existingTerms.add(userEmail);
            bundle.getMetadata().setTerms(existingTerms);
            try {
                update(bundle, auth);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.info("Could not update terms for Provider with id: '{}'", id);
            }
        }
    }

    public CatalogueBundle suspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        CatalogueBundle existingCatalogue = get(id, auth);
        existingCatalogue.markSuspend(suspend, auth);

        // Suspend Catalogue's resources
        List<ProviderBundle> providers = providerService.getAll(createFacetFilter(id), auth).getResults();
        if (providers != null && !providers.isEmpty()) {
            for (ProviderBundle providerBundle : providers) {
                providerService.setSuspend(providerBundle.getId(), id, suspend, auth);
            }
        }

        return super.update(existingCatalogue, auth);
    }

    @Override
    public CatalogueBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyCatalogue with id: '{}' | status: '{}' | active: '{}'", id, status, active);
        CatalogueBundle existingCatalogue = get(id);
        existingCatalogue.markOnboard(status, active, UserInfo.of(auth), null);

        logger.info("Verifying Catalogue: {}", existingCatalogue);
        return super.update(existingCatalogue, auth);
    }

    @Override
    public CatalogueBundle setActive(String id, Boolean active, Authentication auth) {
        CatalogueBundle existingCatalogue = get(id);
        if ((existingCatalogue.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existingCatalogue.getStatus().equals(vocabularyService.get("rejected").getId())) && !existingCatalogue.isActive()) {
            throw new ResourceException(String.format("You cannot activate this Catalogue, because it's Inactive with status = [%s]",
                    existingCatalogue.getStatus()), HttpStatus.CONFLICT);
        }
        existingCatalogue.markActive(active, UserInfo.of(auth));
        return super.update(existingCatalogue, auth);
    }

    public CatalogueBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        CatalogueBundle existingCatalogue = get(id);
        existingCatalogue.markAudit(comment, actionType, auth);
        logger.info("Audited Catalogue '{}' with [actionType: {}]",
                existingCatalogue.getId(), actionType);
        return super.update(existingCatalogue, auth);
    }

    @Override
    public Paging<CatalogueBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    private FacetFilter createFacetFilter(String catalogueId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        return ff;
    }

    @Override
    public CatalogueBundle addDraft(CatalogueBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public CatalogueBundle updateDraft(CatalogueBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public void deleteDraft(CatalogueBundle bundle) {

    }

    @Override
    public CatalogueBundle finalizeDraft(CatalogueBundle catalogueBundle, Authentication auth) {
        return null;
    }
}
