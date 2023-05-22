package eu.einfracentral.registry.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@org.springframework.stereotype.Service
public class DatasourceBundleManager extends AbstractResourceBundleManager<DatasourceBundle> implements ResourceBundleService<DatasourceBundle>, DatasourceService<DatasourceBundle> {

    private static final Logger logger = LogManager.getLogger(DatasourceBundleManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final PublicDatasourceManager publicDatasourceManager;
    private final MigrationService migrationService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${project.catalogue.name}")
    private String catalogueName;
    @Value("${openaire.dsm.api}")
    private String openaireAPI;

    public DatasourceBundleManager(ProviderService<ProviderBundle, Authentication> providerService,
                                   IdCreator idCreator,
                                   @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   CatalogueService<CatalogueBundle, Authentication> catalogueService,
                                   PublicDatasourceManager publicDatasourceManager,
                                   @Lazy MigrationService migrationService,
                                   ProviderResourcesCommonMethods commonMethods) {
        super(DatasourceBundle.class);
        this.providerService = providerService; // for providers
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
        this.publicDatasourceManager = publicDatasourceManager;
        this.migrationService = migrationService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "datasource";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle addResource(DatasourceBundle datasourceBundle, Authentication auth) {
        return addResource(datasourceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle addResource(DatasourceBundle datasourceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.equals("")) { // add catalogue provider
            datasourceBundle.getDatasource().setCatalogueId(catalogueName);
        } else { // add provider from external catalogue
            commonMethods.checkCatalogueIdConsistency(datasourceBundle, catalogueId);
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
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")){
            throw new ValidationException(String.format("The Provider with id %s has already registered a Resource Template.", providerBundle.getId()));
        }

        // if Datasource has ID -> check if it exists in OpenAIRE Datasources list
        if (datasourceBundle.getId() != null && !datasourceBundle.getId().equals("")){
            checkOpenAIREIDExistance(datasourceBundle);
        }
        try {
            datasourceBundle.setId(idCreator.createDatasourceId(datasourceBundle));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")){
            datasourceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            datasourceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            datasourceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // serviceType
        createResourceExtras(datasourceBundle, "service_type-datasource");

        logger.info("Adding Datasource: {}", datasourceBundle);
        DatasourceBundle ret;
        ret = super.add(datasourceBundle, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #datasourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle updateResource(DatasourceBundle datasourceBundle, String comment, Authentication auth) {
        return updateResource(datasourceBundle, datasourceBundle.getDatasource().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #datasourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle updateResource(DatasourceBundle datasourceBundle, String catalogueId, String comment, Authentication auth) {

        DatasourceBundle ret;
        DatasourceBundle existingDatasource;
        try { // try to find a Datasource with the same id
            existingDatasource = get(datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId());
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Datasource with id [%s] on the [%s] Catalogue",
                    datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId()));
        }

        // check if there are actual changes in the Datasource
        if (datasourceBundle.getDatasource().equals(existingDatasource.getDatasource())){
            throw new ValidationException("There are no changes in the Datasource", HttpStatus.OK);
        }

        if (catalogueId == null || catalogueId.equals("")) {
            datasourceBundle.getDatasource().setCatalogueId(catalogueName);
        } else {
            commonMethods.checkCatalogueIdConsistency(datasourceBundle, catalogueId);
        }

        logger.trace("User '{}' is attempting to update the Datasource with id '{}' of the Catalogue '{}'", auth, datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId());
        validate(datasourceBundle);

        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getDatasource().getResourceOrganisation(), auth);

        // if service version is empty set it null
        if ("".equals(datasourceBundle.getDatasource().getVersion())) {
            datasourceBundle.getDatasource().setVersion(null);
        }

        // block Public Datasource updates
        if (existingDatasource.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Datasource");
        }

        User user = User.of(auth);

        // update existing datasource Metadata, ResourceExtras, Identifiers, MigrationStatus
        datasourceBundle.setMetadata(Metadata.updateMetadata(existingDatasource.getMetadata(), user.getFullName()));
        datasourceBundle.setResourceExtras(existingDatasource.getResourceExtras());
//        datasourceBundle.setIdentifiers(existingDatasource.getIdentifiers());
        datasourceBundle.setMigrationStatus(existingDatasource.getMigrationStatus());

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
            if (existingDatasource.getLoggingInfo() != null) {
                loggingInfoList = existingDatasource.getLoggingInfo();
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
            } else {
                loggingInfoList.add(loggingInfo);
            }
        }
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        datasourceBundle.setLatestUpdateInfo(loggingInfo);
        datasourceBundle.setActive(existingDatasource.isActive());

        // set status
        datasourceBundle.setStatus(existingDatasource.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingDatasource.getStatus().equals(vocabularyService.get("rejected resource").getId())){
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())){
                datasourceBundle.setStatus(vocabularyService.get("pending resource").getId());
                datasourceBundle.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
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
                    securityService.userIsResourceProviderAdmin(user, datasourceId, catalogueId)) {
                return datasourceBundle;
            }
        }
        // else return the Service ONLY if it is active
        if (datasourceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())){
            return datasourceBundle;
        }
        throw new ValidationException("You cannot view the specific Datasource");
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        // block Public Datasource deletion
        if (datasourceBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Service");
        }
        logger.info("Deleting Datasource: {}", datasourceBundle);
        super.delete(datasourceBundle);
    }


    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        String[] parts = id.split("\\.");
        String providerId = parts[0];
        DatasourceBundle datasourceBundle = null;
        List<DatasourceBundle> datasourceBundles = getResourceBundles(providerId, auth);
        for (DatasourceBundle service : datasourceBundles){
            if (service.getDatasource().getId().equals(id)){
                datasourceBundle = service;
            }
        }
        if (datasourceBundle == null){
            throw new ValidationException(String.format("The Resource with id '%s' does not exist", id));
        }
        datasourceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getDatasource().getResourceOrganisation(), auth);
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        User user = User.of(auth);

        if (datasourceBundle.getLoggingInfo() != null) {
            loggingInfoList = datasourceBundle.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                datasourceBundle.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                datasourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                datasourceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                datasourceBundle.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                datasourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                datasourceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        logger.info("Verifying Resource: {}", datasourceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return super.update(datasourceBundle, auth);
    }

    @Override
    public DatasourceBundle publish(String resourceId, Boolean active, Authentication auth) {
        DatasourceBundle datasourceBundle;
        String activeProvider = "";
        datasourceBundle = this.get(resourceId, catalogueName);

        if ((datasourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                datasourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !datasourceBundle.isActive()){
            throw new ValidationException(String.format("You cannot activate this Resource, because it's Inactive with status = [%s]", datasourceBundle.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getDatasource().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = datasourceBundle.getDatasource().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        datasourceBundle.setActive(active);

        User user = User.of(auth);
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        if (active){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.ACTIVATED.getKey());
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DEACTIVATED.getKey());
        }
        if (datasourceBundle.getLoggingInfo() != null){
            loggingInfoList = datasourceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        }
        else{
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
            loggingInfoList.add(loggingInfo);
        }
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        datasourceBundle.setLatestUpdateInfo(loggingInfo);

        this.update(datasourceBundle, auth);
        return datasourceBundle;
    }

    public DatasourceBundle auditResource(String resourceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        DatasourceBundle resource = get(resourceId, catalogueName);
        User user = User.of(auth);
        LoggingInfo loggingInfo; // TODO: extract method
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (resource.getLoggingInfo() != null) {
            loggingInfoList = resource.getLoggingInfo();
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(), actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        resource.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        resource.setLatestAuditInfo(loggingInfo);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForResourceAuditing(resource);

        logger.info("Auditing Resource: {}", resource);
        return super.update(resource, auth);
    }

    @Override
    public List<DatasourceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<DatasourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<Datasource> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Services
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle.getId(), providerBundle.getProvider().getCatalogueId())) {
                return this.getAll(ff, auth).getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
            }
        }
        // else return Provider's Services ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return this.getAll(ff, null).getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Datasources of the specific Provider");
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Datasource> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
    }

    //    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        DatasourceBundle datasourceBundle = new DatasourceBundle();
        try{
            datasourceBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources){
                DatasourceBundle service = deserialize(resource);
                if (service.getLoggingInfo() != null){
                    loggingInfoList.addAll(service.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch(ResourceNotFoundException e){
            logger.info(String.format("Resource with id [%s] not found", id));
        }
        return null;
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth){
        String providerId = providerService.get(get(resourceId).getDatasource().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getDatasource().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public DatasourceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth){
        DatasourceBundle datasourceBundle = get(resourceId, catalogueName);
        // check Datasource's status
        if (!datasourceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("You cannot move Datasource with id [%s] to another Provider as it" +
                    "is not yet Approved", datasourceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(catalogueName, newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(catalogueName, datasourceBundle.getDatasource().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())){
            throw new ValidationException("You cannot move a Datasource to a Provider of another Catalogue");
        }

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = datasourceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || "".equals(comment)) {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        datasourceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = datasourceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        datasourceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        datasourceBundle.getDatasource().setResourceOrganisation(newProviderId);

        // update ResourceProviders
        List<String> resourceProviders = datasourceBundle.getDatasource().getResourceProviders();
        if (resourceProviders.contains(oldProvider.getId())){
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // update id
        try {
            datasourceBundle.setId(idCreator.createDatasourceId(datasourceBundle));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // add Resource, delete the old one
        add(datasourceBundle, auth);
        publicDatasourceManager.delete(get(resourceId, catalogueName)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueName));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, datasourceBundle.getId());

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, datasourceBundle, auth);

        return datasourceBundle;
    }

    public Map<Integer, List<Datasource>> getAllOpenAIREDatasources(FacetFilter ff) {
        Map<Integer, List<Datasource>> datasourceMap = new HashMap<>();
        List<Datasource> allDatasources = new ArrayList<>();
        String[] datasourcesAsJSON = getOpenAIREDatasourcesAsJSON(ff);
        int total = Integer.parseInt(datasourcesAsJSON[0]);
        String allOpenAIREDatasources = datasourcesAsJSON[1];
        if (allOpenAIREDatasources != null){
            JSONObject obj = new JSONObject(allOpenAIREDatasources);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            for(int i = 0; i < arr .length(); i++) {
                JSONObject map = arr.getJSONObject(i);
                Gson gson = new Gson();
                JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                Datasource datasource = transformOpenAIREToEOSCDatasource(jsonObj);
                if (datasource != null){
                    allDatasources.add(datasource);
                }
            }
            datasourceMap.put(total, allDatasources);
            return datasourceMap;
        }
        throw new ResourceNotFoundException("There are no OpenAIRE Datasources");
    }

    public String[] getOpenAIREDatasourcesAsJSON(FacetFilter ff) {
        String[] pagination = createPagination(ff);
        int page = Integer.parseInt(pagination[0]);
        int quantity = Integer.parseInt(pagination[1]);
        String ordering = pagination[2];
        String data = pagination[3];
        String url = openaireAPI+"openaire/ds/searchdetails/"+page+"/"+quantity+"?order="+ordering+"&requestSortBy=id";
        String response = createHttpRequest(url, data);
        if (response != null){
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            String total = jsonObj.getAsJsonObject().get("header").getAsJsonObject().get("total").toString();
            jsonObj.getAsJsonObject().remove("header");
            return new String[]{total, jsonObj.toString()};
        }
        return new String[]{};
    }

    private String[] createPagination(FacetFilter ff){
        int page;
        int quantity = ff.getQuantity();
        if (ff.getFrom() >= quantity){
            page = ff.getFrom() / quantity;
        } else {
            page = ff.getFrom() / 10;
        }
        String ordering = "ASCENDING";
        if (ff.getOrderBy() != null){
            String order = ff.getOrderBy().get(ff.getOrderBy().keySet().toArray()[0]).toString();
            if (order.contains("desc")){
                ordering = "DESCENDING";
            }
        }
        String data = "{}";
        if (ff.getFilter() != null && !ff.getFilter().isEmpty()){
            page = 0;
            quantity = 10;
            if (ff.getFilter().containsKey("id")){
                data = "{  \"id\": \""+ff.getFilter().get("id")+"\"}";
            }
        }
        if (ff.getKeyword() != null && !ff.getKeyword().equals("")){
            data = "{  \"officialname\": \""+ff.getKeyword()+"\"}";
        }
        return new String[]{Integer.toString(page), Integer.toString(quantity), ordering, data};
    }

    public String createHttpRequest(String url, String data){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "application/json");
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(data, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }

    public Datasource getOpenAIREDatasourceById(String openaireDatasourceID) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", openaireDatasourceID);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        if (datasource != null){
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    return transformOpenAIREToEOSCDatasource(jsonObj);
                }
            }
        }
        throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
    }

    public Datasource transformOpenAIREToEOSCDatasource(JsonElement openaireDatasource){
        Datasource datasource = new Datasource();
        String id = openaireDatasource.getAsJsonObject().get("id").getAsString().replaceAll("\"", "");
        String name = openaireDatasource.getAsJsonObject().get("officialname").getAsString().replaceAll("\"", "");
        datasource.setId(id);
        datasource.setName(name);
        return datasource;
    }

    private String getOpenAIREDatasourceRegisterBy(String openaireDatasourceID) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", openaireDatasourceID);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        String registerBy = null;
        if (datasource != null){
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    try {
                        registerBy = jsonObj.getAsJsonObject().get("registeredby").getAsString();
                    } catch (UnsupportedOperationException e) {
                        logger.error(e);
                    }
                }
            }
        }
        return registerBy;
    }

    public DatasourceBundle checkOpenAIREIDExistance(DatasourceBundle datasourceBundle){
        Datasource datasource = getOpenAIREDatasourceById(datasourceBundle.getId());
        if (datasource != null){
            Identifiers datasourceIdentifiers = new Identifiers();
            List<AlternativeIdentifier> datasourceAlternativeIdentifiers = new ArrayList<>();
            AlternativeIdentifier alternativeIdentifier = new AlternativeIdentifier();
            alternativeIdentifier.setType("openaire");
            alternativeIdentifier.setValue(datasourceBundle.getId());
            datasourceAlternativeIdentifiers.add(alternativeIdentifier);
            datasourceIdentifiers.setAlternativeIdentifiers(datasourceAlternativeIdentifiers);
            datasourceBundle.setIdentifiers(datasourceIdentifiers);
        } else{
            throw new ValidationException(String.format("The ID [%s] you provided does not belong to an OpenAIRE Datasource", datasourceBundle.getId()));
        }
        return datasourceBundle;
    }

    public boolean isDatasourceRegisteredOnOpenAIRE(String eoscId){
        DatasourceBundle datasourceBundle = get(eoscId);
        boolean found = false;
        String registerBy;
        if (datasourceBundle != null){
            Identifiers identifiers = datasourceBundle.getIdentifiers();
            if (identifiers != null){
                List<AlternativeIdentifier> alternativeIdentifiers = identifiers.getAlternativeIdentifiers();
                if (alternativeIdentifiers != null && !alternativeIdentifiers.isEmpty()){
                    for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers){
                        if (alternativeIdentifier.getType().equals("openaire")){
                            registerBy = getOpenAIREDatasourceRegisterBy(alternativeIdentifier.getValue());
                            if(registerBy != null && !registerBy.equals("")){
                                found = true;
                            }
                        }
                    }
                }
            }
        } else{
            throw new ResourceNotFoundException(String.format("There is no Datasource with ID [%s]", eoscId));
        }
        return found;
    }

    public DatasourceBundle createPublicResource(DatasourceBundle datasourceBundle, Authentication auth){
        publicDatasourceManager.add(datasourceBundle, auth);
        return datasourceBundle;
    }
}
