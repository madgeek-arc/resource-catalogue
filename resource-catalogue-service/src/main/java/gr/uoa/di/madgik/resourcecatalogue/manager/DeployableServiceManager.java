/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.*;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;
import static java.util.stream.Collectors.toList;

@Service
public class DeployableServiceManager extends ResourceCatalogueManager<DeployableServiceBundle> implements DeployableServiceService {

    private static final Logger logger = LoggerFactory.getLogger(DeployableServiceManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final CatalogueService catalogueService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicDeployableServiceService publicDeployableServiceService;

    @Autowired
    private FacetLabelService facetLabelService;
    @Autowired
    private FieldValidator fieldValidator;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DeployableServiceManager(ProviderService providerService,
                                    IdCreator idCreator, @Lazy SecurityService securityService,
                                    @Lazy VocabularyService vocabularyService,
                                    CatalogueService catalogueService,
                                    PublicDeployableServiceService publicDeployableServiceService,
                                    @Lazy ProviderResourcesCommonMethods commonMethods) {
        super(DeployableServiceBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
        this.publicDeployableServiceService = publicDeployableServiceService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceTypeName() {
        return "deployable_service";
    }

    @Override
    public DeployableServiceBundle add(DeployableServiceBundle deployableServiceBundle, Authentication auth) {
        return add(deployableServiceBundle, null, auth);
    }

    @Override
    public DeployableServiceBundle add(DeployableServiceBundle deployableServiceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) { // add catalogue deployable service
            deployableServiceBundle.getDeployableService().setCatalogueId(this.catalogueId);
            deployableServiceBundle.setId(idCreator.generate(getResourceTypeName()));
            commonMethods.createIdentifiers(deployableServiceBundle, getResourceTypeName(), false);
        } else { // add deployable service from external catalogue
            commonMethods.checkCatalogueIdConsistency(deployableServiceBundle, catalogueId);
            idCreator.validateId(deployableServiceBundle.getId());
            commonMethods.createIdentifiers(deployableServiceBundle, getResourceTypeName(), true);
        }

        ProviderBundle providerBundle = providerService.get(deployableServiceBundle.getDeployableService().getCatalogueId(),
                deployableServiceBundle.getDeployableService().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new CatalogueResourceNotFoundException(String.format("Provider with id '%s' and catalogueId '%s' does not exist",
                    deployableServiceBundle.getDeployableService().getResourceOrganisation(), deployableServiceBundle.getDeployableService().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    deployableServiceBundle.getDeployableService().getResourceOrganisation()), HttpStatus.CONFLICT);
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource Template.",
                    providerBundle.getId()), HttpStatus.CONFLICT);
        }
        validateDeployableService(deployableServiceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        deployableServiceBundle.setActive(active);

        // create new Metadata if not exists
        if (deployableServiceBundle.getMetadata() == null) {
            deployableServiceBundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth)));
        }

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(deployableServiceBundle, auth);

        // latestOnboardingInfo
        deployableServiceBundle.setLatestOnboardingInfo(loggingInfoList.getFirst());

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            deployableServiceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            deployableServiceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            deployableServiceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        deployableServiceBundle.setLoggingInfo(loggingInfoList);
        deployableServiceBundle.setAuditState(Auditable.NOT_AUDITED);

        logger.info("Adding Deployable Service: {}", deployableServiceBundle);
        DeployableServiceBundle ret;

        ret = super.add(deployableServiceBundle, auth);

        return ret;
    }

    @Override
    public DeployableServiceBundle update(DeployableServiceBundle deployableServiceBundle, String comment, Authentication auth) {
        return update(deployableServiceBundle, deployableServiceBundle.getDeployableService().getCatalogueId(), comment, auth);
    }

    @Override
    public DeployableServiceBundle update(DeployableServiceBundle deployableServiceBundle, String catalogueId, String comment, Authentication auth) {

        DeployableServiceBundle ret = ObjectUtils.clone(deployableServiceBundle);
        DeployableServiceBundle existingDeployableService;
        existingDeployableService = get(ret.getDeployableService().getId(), ret.getDeployableService().getCatalogueId(), false);
        if (ret.getDeployableService().equals(existingDeployableService.getDeployableService())) {
            return ret;
        }

        if (catalogueId == null || catalogueId.isEmpty()) {
            ret.getDeployableService().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }

        logger.trace("Attempting to update the Deployable Service with id '{}' of the Catalogue '{}'",
                ret.getDeployableService().getId(), ret.getDeployableService().getCatalogueId());
        validateDeployableService(ret);

        ProviderBundle providerBundle = providerService.get(ret.getDeployableService().getCatalogueId(),
                ret.getDeployableService().getResourceOrganisation(), auth);

        // block Public Deployable Service update
        if (existingDeployableService.getMetadata().isPublished()) {
            throw new ResourceException("You cannot directly update a Public Deployable Service", HttpStatus.FORBIDDEN);
        }

        // update existing DeployableService Metadata, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingDeployableService.getMetadata(), AuthenticationInfo.getFullName(auth)));
        ret.setIdentifiers(existingDeployableService.getIdentifiers());
        ret.setMigrationStatus(existingDeployableService.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingDeployableService, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // set active/status
        ret.setActive(existingDeployableService.isActive());
        ret.setStatus(existingDeployableService.getStatus());
        ret.setSuspended(existingDeployableService.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingDeployableService.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                ret.setStatus(vocabularyService.get("pending resource").getId());
                ret.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingDeployableService.getDeployableService().getCatalogueId().equals(ret.getDeployableService().getCatalogueId())) {
                throw new ResourceException("You cannot change catalogueId", HttpStatus.FORBIDDEN);
            }
        }

        logger.info("Updating Deployable Service: {}", ret);
        ret = super.update(ret, auth);

        return ret;
    }

    @Override
    public Browsing<DeployableServiceBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        filter.addFilter("resource_organisation", providers.stream().map(ProviderBundle::getId).toList());
        filter.setResourceType(getResourceTypeName());
        return this.getAll(filter, auth);
    }

    @Override
    public DeployableServiceBundle getCatalogueResource(String catalogueId, String deployableServiceId, Authentication auth) {
        DeployableServiceBundle deployableServiceBundle = get(deployableServiceId, catalogueId, false);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(catalogueId, "Catalogue");
        }
        if (!deployableServiceBundle.getDeployableService().getCatalogueId().equals(catalogueId)) {
            throw new CatalogueResourceNotFoundException(String.format("Deployable Service with id [%s] does not belong to the" +
                    " catalogue with id [%s]", deployableServiceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceAdmin(user, deployableServiceId)) {
                return deployableServiceBundle;
            }
        }
        // else return the Deployable Service ONLY if it is active
        if (deployableServiceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return deployableServiceBundle;
        }
        throw new InsufficientAuthenticationException("You cannot view the specific Deployable Service");
    }

    @Override
    public void delete(DeployableServiceBundle deployableServiceBundle) {
        String catalogueId = deployableServiceBundle.getDeployableService().getCatalogueId();
        commonMethods.blockResourceDeletion(deployableServiceBundle.getStatus(), deployableServiceBundle.getMetadata().isPublished());
        commonMethods.deleteResourceRelatedServiceExtensionsAndResourceInteroperabilityRecords(deployableServiceBundle.getId(), catalogueId, "deployable_service");
        logger.info("Deleting Deployable Service: {}", deployableServiceBundle);
        super.delete(deployableServiceBundle);
    }

    public DeployableServiceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource state!", status));
        }
        logger.trace("verifyResource with id: '{}' | status: '{}' | active: '{}'", id, status, active);
        DeployableServiceBundle deployableServiceBundle = get(id, catalogueId, false);
        deployableServiceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(deployableServiceBundle.getDeployableService().getResourceOrganisation(),
                deployableServiceBundle.getDeployableService().getCatalogueId(), false);
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(deployableServiceBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                deployableServiceBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                deployableServiceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                deployableServiceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                deployableServiceBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                deployableServiceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                deployableServiceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }

        logger.info("Verifying Deployable Service: {}", deployableServiceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return update(deployableServiceBundle, auth);
    }

    public void validateDeployableService(DeployableServiceBundle deployableServiceBundle) {
        DeployableService deployableService = deployableServiceBundle.getDeployableService();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Deployable Service with id: {}", deployableService.getId());

        try {
            fieldValidator.validate(deployableServiceBundle);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
        if (deployableServiceBundle.getDeployableService().getScientificDomains() != null &&
                !deployableServiceBundle.getDeployableService().getScientificDomains().isEmpty()) {
            validateScientificDomains(deployableServiceBundle.getDeployableService().getScientificDomains());
        }

    }

    @Override
    public DeployableServiceBundle publish(String deployableServiceId, Boolean active, Authentication auth) {
        DeployableServiceBundle deployableServiceBundle;
        String activeProvider = "";
        deployableServiceBundle = get(deployableServiceId, catalogueId, false);

        if ((deployableServiceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                deployableServiceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !deployableServiceBundle.isActive()) {
            throw new ResourceException(String.format("You cannot activate this Deployable Service, because it's Inactive with status = [%s]",
                    deployableServiceBundle.getStatus()), HttpStatus.CONFLICT);
        }

        ProviderBundle providerBundle = providerService.get(deployableServiceBundle.getDeployableService().getResourceOrganisation(),
                deployableServiceBundle.getDeployableService().getCatalogueId(), false);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = deployableServiceBundle.getDeployableService().getResourceOrganisation();
        }
        if (active && activeProvider.isEmpty()) {
            throw new ResourceException("Deployable Service does not have active Providers", HttpStatus.CONFLICT);
        }
        deployableServiceBundle.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(deployableServiceBundle, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        deployableServiceBundle.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        deployableServiceBundle.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        deployableServiceBundle.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        deployableServiceBundle.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        update(deployableServiceBundle, auth);
        return deployableServiceBundle;
    }

    @Override
    public DeployableServiceBundle audit(String deployableServiceId, String catalogueId, String comment,
                                         LoggingInfo.ActionType actionType, Authentication auth) {
        DeployableServiceBundle deployableService = get(deployableServiceId, catalogueId, false);
        ProviderBundle provider = providerService.get(deployableService.getDeployableService().getResourceOrganisation(),
                deployableService.getDeployableService().getCatalogueId(), false);
        commonMethods.auditResource(deployableService, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            deployableService.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            deployableService.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }

        logger.info("User '{}-{}' audited Deployable Service '{}'-'{}' with [actionType: {}]",
                AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase(),
                deployableService.getDeployableService().getId(), deployableService.getDeployableService().getName(), actionType);
        return super.update(deployableService, auth);
    }

    @Override
    public List<DeployableServiceBundle> getResourceBundles(String providerId, Authentication auth) {
        return getResourceBundles(catalogueId, providerId, auth).getResults();
    }

    @Override
    public Paging<DeployableServiceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<DeployableServiceBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, null).getResults();
    }

    @Override
    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        //todo?
    }

    @Override
    public DeployableServiceBundle createPublicResource(DeployableServiceBundle deployableServiceBundle, Authentication auth) {
        publicDeployableServiceService.add(deployableServiceBundle, auth);
        return deployableServiceBundle;
    }


    @Override
    public DeployableServiceBundle getOrElseReturnNull(String id) {
        DeployableServiceBundle deployableServiceBundle;
        try {
            deployableServiceBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return deployableServiceBundle;
    }

    @Override
    public Browsing<DeployableServiceBundle> getAll(FacetFilter ff, Authentication auth) {
        updateFacetFilterConsideringTheAuthorization(ff, auth);

        Browsing<DeployableServiceBundle> resources;
        resources = getResults(ff);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.generateLabels(resources.getFacets()));
        }
        return resources;
    }

    private void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth) {
        // if user is Unauthorized, return active ONLY
        if (auth == null || !auth.isAuthenticated() || (
                !securityService.hasRole(auth, "ROLE_PROVIDER") &&
                        !securityService.hasRole(auth, "ROLE_EPOT") &&
                        !securityService.hasRole(auth, "ROLE_ADMIN"))) {
            filter.addFilter("active", true);
            filter.addFilter("published", false);
        }
    }

    @Override
    public Paging<DeployableServiceBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved resource");
        facetFilter.addFilter("published", false);

        Browsing<DeployableServiceBundle> dsBrowsing = getAll(facetFilter, auth);
        List<DeployableServiceBundle> dsToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(auditingInterval)
                .toEpochSecond();

        for (DeployableServiceBundle deployableServiceBundle : dsBrowsing.getResults()) {
            LoggingInfo auditInfo = deployableServiceBundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include services that have never been audited
                dsToBeAudited.add(deployableServiceBundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include services that were last audited before the threshold
                        dsToBeAudited.add(deployableServiceBundle);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(dsToBeAudited);

        // Limit the list to the requested quantity
        if (dsToBeAudited.size() > quantity) {
            dsToBeAudited = dsToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(dsToBeAudited.size(), 0, dsToBeAudited.size(), dsToBeAudited,
                dsBrowsing.getFacets());
    }

    @Override
    public DeployableServiceBundle getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<DeployableServiceBundle> allProviderDeployableServices = getAll(ff, auth).getResults();
        for (DeployableServiceBundle deployableServiceBundle : allProviderDeployableServices) {
            if (deployableServiceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return deployableServiceBundle;
            }
        }
        return null;
    }

    @Override
    public List<DeployableService> getByIds(Authentication auth, String... ids) {
        List<DeployableService> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId, false).getDeployableService();
                    } catch (CatalogueResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
    }

    @Override
    public DeployableServiceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        DeployableServiceBundle deployableServiceBundle = get(resourceId, catalogueId, false);
        if (!deployableServiceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Deployable Service with id [%s] to another Provider as it" +
                    "is not yet Approved", deployableServiceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(deployableServiceBundle.getDeployableService().getCatalogueId(),
                deployableServiceBundle.getDeployableService().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot move a Deployable Service to a Provider of another Catalogue");
        }

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = deployableServiceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment.isEmpty()) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        deployableServiceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        deployableServiceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = deployableServiceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(AuthenticationInfo.getFullName(auth));
        metadata.setTerms(null);
        deployableServiceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        deployableServiceBundle.getDeployableService().setResourceOrganisation(newProviderId);

        // add Resource, delete the old one
        add(deployableServiceBundle, auth);
        publicDeployableServiceService.delete(get(resourceId, catalogueId, false)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueId, false));

        return deployableServiceBundle;
    }

    @Override
    public DeployableServiceBundle suspend(String deployableServiceId, String catalogueId, boolean suspend, Authentication auth) {
        DeployableServiceBundle deployableServiceBundle = get(deployableServiceId, catalogueId, false);
        commonMethods.suspensionValidation(deployableServiceBundle, deployableServiceBundle.getDeployableService().getCatalogueId(),
                deployableServiceBundle.getDeployableService().getResourceOrganisation(), suspend, auth);
        commonMethods.suspendResource(deployableServiceBundle, suspend, auth);
        return super.update(deployableServiceBundle, auth);
    }
}
