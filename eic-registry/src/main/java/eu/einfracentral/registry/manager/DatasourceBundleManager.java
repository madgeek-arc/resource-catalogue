package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@org.springframework.stereotype.Service
public class DatasourceBundleManager extends AbstractResourceBundleManager<DatasourceBundle> implements ResourceBundleService<DatasourceBundle> {

    private static final Logger logger = LogManager.getLogger(DatasourceBundleManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    public DatasourceBundleManager(ProviderService<ProviderBundle, Authentication> providerService,
                                   Random randomNumberGenerator, IdCreator idCreator,
                                   @Lazy FieldValidator fieldValidator,
                                   @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(DatasourceBundle.class);
        this.providerService = providerService; // for providers
        this.randomNumberGenerator = randomNumberGenerator;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
    }

    @Override
    public String getResourceType() {
        return "datasource";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle addResource(DatasourceBundle datasourceBundle, Authentication auth) {
        return addResource(datasourceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle addResource(DatasourceBundle datasourceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null) { // add catalogue provider
            datasourceBundle.getDatasource().setCatalogueId(catalogueName);
        } else { // add provider from external catalogue
            checkCatalogueIdConsistency(datasourceBundle, catalogueId);
        }

        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getDatasource().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' and catalogueId '%s' does not exist", datasourceBundle.getDatasource().getResourceOrganisation(), datasourceBundle.getDatasource().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")){
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    datasourceBundle.getDatasource().getResourceOrganisation()));
        }

        // create ID if not exists
        if ((datasourceBundle.getDatasource().getId() == null) || ("".equals(datasourceBundle.getDatasource().getId()))) {
            String id = idCreator.createResourceId(datasourceBundle.getPayload());
            datasourceBundle.getDatasource().setId(id);
        }
        validate(datasourceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        datasourceBundle.setActive(active);

        // create new Metadata if not exists
        if (datasourceBundle.getMetadata() == null) {
            datasourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        datasourceBundle.setLatestOnboardingInfo(loggingInfo);

        sortFields(datasourceBundle);

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")){
            datasourceBundle.setStatus(vocabularyService.get("approved datasource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            datasourceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            datasourceBundle.setStatus(vocabularyService.get("pending datasource").getId());
        }

        // LoggingInfo
        datasourceBundle.setLoggingInfo(loggingInfoList);

        logger.info("Adding Datasource: {}", datasourceBundle);
        DatasourceBundle ret;
        ret = super.add(datasourceBundle, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle updateResource(DatasourceBundle datasourceBundle, String comment, Authentication auth) {
        return updateResource(datasourceBundle, datasourceBundle.getDatasource().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle updateResource(DatasourceBundle datasourceBundle, String catalogueId, String comment, Authentication auth) {
        DatasourceBundle ret;

        if (catalogueId == null) {
            datasourceBundle.getDatasource().setCatalogueId(catalogueName);
        } else {
            checkCatalogueIdConsistency(datasourceBundle, catalogueId);
        }

        logger.trace("User '{}' is attempting to update the Datasource with id '{}' of the Catalogue '{}'", auth, datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId());
        validate(datasourceBundle);
        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getDatasource().getResourceOrganisation(), auth);
        DatasourceBundle existingDatasource;

        // if service version is empty set it null
        if ("".equals(datasourceBundle.getDatasource().getVersion())) {
            datasourceBundle.getDatasource().setVersion(null);
        }

        try { // try to find a Datasource with the same id
            existingDatasource = get(datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId());
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Datasource with id [%s] on the [%s] Catalogue",
                    datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId()));
        }

        User user = User.of(auth);

        // update existing service serviceMetadata
        datasourceBundle.setMetadata(Metadata.updateMetadata(existingDatasource.getMetadata(), user.getFullName()));
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        // update VS version update
        if (((datasourceBundle.getDatasource().getVersion() == null) && (existingDatasource.getDatasource().getVersion() == null)) ||
                (datasourceBundle.getDatasource().getVersion().equals(existingDatasource.getDatasource().getVersion()))){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
            if (existingDatasource.getLoggingInfo() != null){
                loggingInfoList = existingDatasource.getLoggingInfo();
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
            } else{
                loggingInfoList.add(loggingInfo);
            }
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
            loggingInfoList.add(loggingInfo);
        }
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        datasourceBundle.setLatestUpdateInfo(loggingInfo);
        datasourceBundle.setActive(existingDatasource.isActive());
        sortFields(datasourceBundle);

        // set status
        datasourceBundle.setStatus(existingDatasource.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingDatasource.getStatus().equals(vocabularyService.get("rejected datasource").getId())){
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())){
                datasourceBundle.setStatus(vocabularyService.get("pending datasource").getId());
                datasourceBundle.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, datasourceBundle.getDatasource().getCatalogueId(), auth);
            }
        }

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingDatasource.getDatasource().getVersion() != null && datasourceBundle.getDatasource().getVersion() == null) {
            throw new ServiceException("You cannot update a Datasource registered with version to a Datasource with null version");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")){
            if (!existingDatasource.getDatasource().getCatalogueId().equals(datasourceBundle.getDatasource().getCatalogueId())){
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        ret = super.update(datasourceBundle, auth);
        logger.info("Updating Datasource: {}", datasourceBundle);

        // send notification emails to Portal Admins
        if (datasourceBundle.getLatestAuditInfo() != null && datasourceBundle.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(datasourceBundle.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(datasourceBundle.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && datasourceBundle.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(datasourceBundle);
            }
        }

        return ret;
    }

    public DatasourceBundle getCatalogueResource(String catalogueId, String datasourceId, Authentication auth) {
        DatasourceBundle datasourceBundle = get(datasourceId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (datasourceBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Datasource with id: %s", datasourceId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!datasourceBundle.getDatasource().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Datasource with id [%s] does not belong to the catalogue with id [%s]", datasourceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            //TODO: userIsCatalogueAdmin -> transcationRollback error
            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsServiceProviderAdmin(user, datasourceId)) {
                return datasourceBundle;
            }
        }
        // else return the Service ONLY if it is active
        if (datasourceBundle.getStatus().equals(vocabularyService.get("approved datasource").getId())){
            return datasourceBundle;
        }
        throw new ValidationException("You cannot view the specific Datasource");
    }
    @Override
    public Paging<DatasourceBundle> getInactiveResources() {
        return null;
    }

    @Override
    public List<Service> createFeaturedResources() {
        return null;
    }

    @Override
    public DatasourceBundle publish(String serviceId, boolean active, Authentication auth) {
        return null;
    }

    @Override
    public DatasourceBundle auditResource(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        return null;
    }

    @Override
    public Paging<DatasourceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        return null;
    }

    @Override
    public List<DatasourceBundle> getResourceBundles(String providerId, Authentication auth) {
        return null;
    }

    @Override
    public Paging<DatasourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        return null;
    }

    @Override
    public List<Service> getServices(String providerId, Authentication auth) {
        return null;
    }

    @Override
    public List<Service> getActiveResources(String providerId) {
        return null;
    }

    @Override
    public DatasourceBundle getResourceTemplate(String providerId, Authentication auth) {
        return null;
    }

    @Override
    public Service getFeaturedService(String providerId) {
        return null;
    }

    @Override
    public List<DatasourceBundle> getInactiveResources(String providerId) {
        return null;
    }

    @Override
    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {

    }

    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        return null;
    }

    @Override
    public DatasourceBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public DatasourceBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth) {
        return null;
    }
}
