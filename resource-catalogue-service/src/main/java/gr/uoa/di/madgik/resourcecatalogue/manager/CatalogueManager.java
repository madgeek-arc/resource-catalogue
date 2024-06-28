package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;

@Service("catalogueManager")
public class CatalogueManager extends ResourceManager<CatalogueBundle> implements CatalogueService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueManager.class);
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final IdCreator idCreator;
    private final FieldValidator fieldValidator;
    private final RegistrationMailService registrationMailService;
    private final ProviderService providerService;
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public CatalogueManager(IdCreator idCreator,
                            @Lazy ProviderService providerService,
                            @Lazy ServiceBundleService serviceBundleService,
                            @Lazy TrainingResourceService trainingResourceService,
                            @Lazy InteroperabilityRecordService interoperabilityRecordService,
                            @Lazy FieldValidator fieldValidator,
                            @Lazy SecurityService securityService,
                            @Lazy VocabularyService vocabularyService,
                            @Lazy RegistrationMailService registrationMailService,
                            @Lazy ProviderResourcesCommonMethods commonMethods) {
        super(CatalogueBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.registrationMailService = registrationMailService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "catalogue";
    }

    @Override
    public CatalogueBundle get(String id) {
        CatalogueBundle catalogue = super.get(id);
        if (catalogue == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", id));
        }
        return catalogue;
    }

    @Override
    public CatalogueBundle get(String id, Authentication auth) {
        CatalogueBundle catalogueBundle = get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Catalogue Admin on the specific Catalogue, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsCatalogueAdmin(user, id)) {
                return catalogueBundle;
            }
        }
        // else return the Catalogue ONLY if it is active
        if (catalogueBundle.getStatus().equals(vocabularyService.get("approved catalogue").getId())) {
            return catalogueBundle;
        }
        throw new ValidationException("You cannot view the specific Catalogue");
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
            // if user is CATALOGUE ADMIN return all his Catalogues (rejected, pending) with their sensitive data (Users, MainContact) too
            User user = User.of(auth);
            Browsing<CatalogueBundle> catalogues = super.getAll(ff, auth);
            for (CatalogueBundle catalogueBundle : catalogues.getResults()) {
                if (catalogueBundle.getStatus().equals(vocabularyService.get("approved catalogue").getId()) ||
                        securityService.userIsCatalogueAdmin(user, catalogueBundle.getId())) {
                    retList.add(catalogueBundle);
                }
            }
            catalogues.setResults(retList);
            catalogues.setTotal(retList.size());
            catalogues.setTo(retList.size());
            userCatalogues = getMyCatalogues(auth);
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
        ff.addFilter("status", "approved catalogue");
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
        catalogue.setId(idCreator.sanitizeString(catalogue.getCatalogue().getAbbreviation()));
        catalogue.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(catalogue, auth);
        catalogue.setLoggingInfo(loggingInfoList);
        catalogue.setActive(false);
        catalogue.setStatus(vocabularyService.get("pending catalogue").getId());
        catalogue.setAuditState(Auditable.NOT_AUDITED);

        // latestOnboardingInfo
        catalogue.setLatestOnboardingInfo(loggingInfoList.get(0));

        CatalogueBundle ret;
        ret = super.add(catalogue, null);
        logger.debug("Adding Catalogue: {}", catalogue);

        registrationMailService.sendEmailsToNewlyAddedCatalogueAdmins(catalogue, null);

        return ret;
    }

    public CatalogueBundle update(CatalogueBundle catalogueBundle, String comment, Authentication auth) {
        logger.trace("Attempting to update the Catalogue with id '{}'", catalogueBundle);

        CatalogueBundle ret = ObjectUtils.clone(catalogueBundle);
        Resource existingResource = whereID(ret.getId(), true);
        CatalogueBundle existingCatalogue = deserialize(existingResource);
        // check if there are actual changes in the Catalogue
        if (ret.getCatalogue().equals(existingCatalogue.getCatalogue())) {
            return ret;
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        ret.setLatestUpdateInfo(loggingInfo);

        ret.setActive(existingCatalogue.isActive());
        ret.setStatus(existingCatalogue.getStatus());
        ret.setSuspended(existingCatalogue.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));
        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);
        resourceService.updateResource(existingResource);
        logger.debug("Updating Catalogue: {}", ret);

        // Send emails to newly added or deleted Admins
        adminDifferences(ret, existingCatalogue);

        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidCatalogueUpdate(ret);
            }
        }

        return ret;
    }

    @Override
    public CatalogueBundle validate(CatalogueBundle catalogue) {
        logger.debug("Validating Catalogue with id: {}", catalogue.getId());

        if (catalogue.getCatalogue().getScientificDomains() != null && !catalogue.getCatalogue().getScientificDomains().isEmpty()) {
            validateScientificDomains(catalogue.getCatalogue().getScientificDomains());
        }

        try {
            fieldValidator.validate(catalogue);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        return catalogue;
    }

    @Override
    public void delete(CatalogueBundle catalogueBundle) {
        String id = catalogueBundle.getId();

        // Block accidental deletion of main Catalogue
        if (id.equals(catalogueId)) {
            throw new ValidationException(String.format("You cannot delete [%s] Catalogue.", catalogueId));
        }

        // Delete Catalogue along with all its related Resources
        logger.info("Deleting all Catalogue's Providers...");
        deleteCatalogueResources(id, providerService, securityService.getAdminAccess());

        logger.info("Deleting all Catalogue's Services...");
        deleteCatalogueResources(id, serviceBundleService, securityService.getAdminAccess());

        logger.info("Deleting all Catalogue's Training Resources...");
        deleteCatalogueResources(id, trainingResourceService, securityService.getAdminAccess());

        logger.info("Deleting all Catalogue's Interoperability Records...");
        deleteCatalogueResources(id, interoperabilityRecordService, securityService.getAdminAccess());

        logger.info("Deleting Catalogue...");
        super.delete(catalogueBundle);
    }

    @Override
    public List<CatalogueBundle> getMyCatalogues(Authentication auth) {
        if (auth == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }
        User user = User.of(auth);
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth).getResults()
                .stream().map(p -> {
                    if (securityService.userIsCatalogueAdmin(user, p.getId())) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private <T, I extends ResourceCRUDService<T, Authentication>> void deleteCatalogueResources(String id, I service, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
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
        for (User user : existingCatalogue.getCatalogue().getUsers()) {
            existingAdmins.add(user.getEmail().toLowerCase());
        }
        for (User user : updatedCatalogue.getCatalogue().getUsers()) {
            newAdmins.add(user.getEmail().toLowerCase());
        }
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        if (!adminsAdded.isEmpty()) {
            registrationMailService.sendEmailsToNewlyAddedCatalogueAdmins(updatedCatalogue, adminsAdded);
        }
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);
        if (!adminsDeleted.isEmpty()) {
            registrationMailService.sendEmailsToNewlyDeletedCatalogueAdmins(updatedCatalogue, adminsDeleted);
        }
    }

    @Override
    public boolean hasAdminAcceptedTerms(String catalogueId, Authentication auth) {
        CatalogueBundle catalogueBundle = get(catalogueId);
        List<String> userList = new ArrayList<>();
        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            userList.add(user.getEmail().toLowerCase());
        }
        if ((catalogueBundle.getMetadata().getTerms() == null || catalogueBundle.getMetadata().getTerms().isEmpty())) {
            if (userList.contains(User.of(auth).getEmail().toLowerCase())) {
                return false; //pop-up modal
            } else {
                return true; //no modal
            }
        }
        if (!catalogueBundle.getMetadata().getTerms().contains(User.of(auth).getEmail().toLowerCase()) && userList.contains(User.of(auth).getEmail().toLowerCase())) {
            return false; // pop-up modal
        }
        return true; // no modal
    }

    @Override
    public void adminAcceptedTerms(String catalogueId, Authentication auth) {
        update(get(catalogueId), auth);
    }

    @Override
    public CatalogueBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Catalogue state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Catalogue State!", status));
        }
        logger.trace("verifyCatalogue with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        CatalogueBundle catalogue = get(id);
        catalogue.setStatus(vocabularyService.get(status).getId());
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(catalogue, auth);
        LoggingInfo loggingInfo = null;

        switch (status) {
            case "approved catalogue":
                if (active == null) {
                    active = true;
                }
                catalogue.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                break;
            case "rejected catalogue":
                catalogue.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                break;
            default:
                break;
        }
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        catalogue.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        catalogue.setLatestOnboardingInfo(loggingInfo);

        logger.info("Verifying Catalogue: {}", catalogue);
        return super.update(catalogue, auth);
    }

    @Override
    public CatalogueBundle publish(String id, Boolean active, Authentication auth) {
        CatalogueBundle catalogue = get(id);
        if ((catalogue.getStatus().equals(vocabularyService.get("pending catalogue").getId()) ||
                catalogue.getStatus().equals(vocabularyService.get("rejected catalogue").getId())) && !catalogue.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Catalogue, because it's Inactive with status = [%s]", catalogue.getStatus()));
        }
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(catalogue, auth);
        LoggingInfo loggingInfo;

        if (active == null) {
            active = false;
        }
        catalogue.setActive(active);
        if (!active) {
            loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.DEACTIVATED.getKey());
        } else {
            loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
        }
        loggingInfoList.add(loggingInfo);
        catalogue.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        catalogue.setLatestUpdateInfo(loggingInfo);
        catalogue.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        catalogue.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        return super.update(catalogue, auth);
    }

    public CatalogueBundle suspend(String id, boolean suspend, Authentication auth) {
        CatalogueBundle catalogueBundle = get(id, auth);

        // Suspend Catalogue
        commonMethods.suspendResource(catalogueBundle, suspend, auth);
        super.update(catalogueBundle, auth);

        // Suspend Catalogue's resources
        List<ProviderBundle> providers = providerService.getAll(createFacetFilter(id), auth).getResults();

        if (providers != null && !providers.isEmpty()) {
            for (ProviderBundle providerBundle : providers) {
                providerService.suspend(providerBundle.getId(), suspend, auth);
            }
        }

        return catalogueBundle;
    }

    public CatalogueBundle audit(String id, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        CatalogueBundle catalogue = get(id);
        commonMethods.auditResource(catalogue, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            catalogue.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            catalogue.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }
        logger.info("User '{}-{}' audited Catalogue '{}'-'{}' with [actionType: {}]",
                User.of(auth).getFullName(), User.of(auth).getEmail(),
                catalogue.getCatalogue().getId(), catalogue.getCatalogue().getName(), actionType);
        return super.update(catalogue, auth);
    }

    private FacetFilter createFacetFilter(String catalogueId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        return ff;
    }
}
