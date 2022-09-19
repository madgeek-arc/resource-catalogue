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
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
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

    @Value("${project.catalogue.name}")
    private String catalogueName;

    public DatasourceBundleManager(ProviderService<ProviderBundle, Authentication> providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(DatasourceBundle.class);
        this.providerService = providerService; // for providers
        this.idCreator = idCreator;
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasourceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle addResource(DatasourceBundle datasourceBundle, Authentication auth) {
        return addResource(datasourceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasourceBundle)")
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

        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueName)){
            datasourceBundle.setId(idCreator.createResourceId(datasourceBundle));
        } else{
            if (datasourceBundle.getId() == null || "".equals(datasourceBundle.getId())) {
                datasourceBundle.setId(idCreator.createResourceId(datasourceBundle));
            } else{
                datasourceBundle.setId(idCreator.reformatId(datasourceBundle.getId()));
            }
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

        logger.info("Adding Datasource: {}", datasourceBundle);
        DatasourceBundle ret;
        ret = super.add(datasourceBundle, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #datasourceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle updateResource(DatasourceBundle datasourceBundle, String comment, Authentication auth) {
        return updateResource(datasourceBundle, datasourceBundle.getDatasource().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #datasourceBundle)")
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
        sortFields(datasourceBundle);

        // set status
        datasourceBundle.setStatus(existingDatasource.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingDatasource.getStatus().equals(vocabularyService.get("rejected resource").getId())){
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())){
                datasourceBundle.setStatus(vocabularyService.get("pending resource").getId());
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
                    securityService.userIsResourceProviderAdmin(user, datasourceId)) {
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
        ProviderBundle resourceProvider = providerService.get(datasourceBundle.getDatasource().getResourceOrganisation());
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
    public Paging<DatasourceBundle> getInactiveResources() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        return getAll(ff, null);
    }

    @Override
    public DatasourceBundle publish(String resourceId, boolean active, Authentication auth) {
        DatasourceBundle datasourceBundle;
        String activeProvider = "";
        datasourceBundle = this.get(resourceId, catalogueName);

        if ((datasourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                datasourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !datasourceBundle.isActive()){
            throw new ValidationException(String.format("You cannot activate this Resource, because it's Inactive with status = [%s]", datasourceBundle.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getResourceOrganisation());
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

    public Paging<DatasourceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("active", true);
        Browsing<DatasourceBundle> serviceBrowsing = getAll(facetFilter, auth);
        Browsing<DatasourceBundle> ret = serviceBrowsing;
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (DatasourceBundle datasourceBundle : serviceBrowsing.getResults()) {
            if (datasourceBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(datasourceBundle.getLatestAuditInfo().getDate()) > interval) {
                    int index = 0;
                    for (int i=0; i<serviceBrowsing.getResults().size(); i++){
                        if (serviceBrowsing.getResults().get(i).getDatasource().getId().equals(datasourceBundle.getDatasource().getId())){
                            index = i;
                            break;
                        }
                    }
                    ret.getResults().remove(index);
                }
            }
        }
        Collections.shuffle(ret.getResults());
        for (int i = ret.getResults().size() - 1; i > ff.getQuantity() - 1; i--) {
            ret.getResults().remove(i);
        }
        ret.setFrom(ff.getFrom());
        ret.setTo(ret.getResults().size());
        ret.setTotal(ret.getResults().size());
        return ret;
    }

    @Override
    public List<DatasourceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<DatasourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth);
    }

    @Override
    public List<Datasource> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Services
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerId)) {
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
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
    }

    // Different that the one called on migration methods!
    @Override
    public DatasourceBundle getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        List<DatasourceBundle> allProviderServices = getAll(ff, auth).getResults();
        for (DatasourceBundle datasourceBundle : allProviderServices){
            if (datasourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())){
                return datasourceBundle;
            }
        }
        return null;
    }

    @Override
    public List<Service> getActiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("active", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
    }

    @Override
    public List<DatasourceBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults();
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
        ProviderBundle newProvider = providerService.get(newProviderId);
        ProviderBundle oldProvider =  providerService.get(datasourceBundle.getDatasource().getResourceOrganisation());

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = datasourceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment == ""){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey());
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // update metadata
        Metadata metadata = datasourceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        datasourceBundle.setMetadata(metadata);

        // update id
        String initialId = datasourceBundle.getId();
        String[] parts = initialId.split("\\.");
        String serviceId = parts[1];
        String newResourceId = newProvider.getId()+"."+serviceId;
        datasourceBundle.setId(newResourceId);
        datasourceBundle.getDatasource().setId(newResourceId);

        // update ResourceOrganisation
        datasourceBundle.getDatasource().setResourceOrganisation(newProvider.getId());

        // add Resource, delete the old one
        add(datasourceBundle, auth);
        delete(get(resourceId, catalogueName));

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
                allDatasources.add(transformOpenAIREToEOSCDatasource(jsonObj));
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
        // PROD -> https://dev-openaire.d4science.org/openaire/ds/searchdetails/0/1000?order=ASCENDING&requestSortBy=dateofvalidation
        String url = "https://beta.services.openaire.eu/openaire/ds/searchdetails/"+page+"/"+quantity+"?order="+ordering+"&requestSortBy=id";
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

    public ResponseEntity<Datasource> getOpenAIREDatasourceById(String datasourceId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", datasourceId);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        if (datasource != null){
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null && arr.length() == 0){
                throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", datasourceId));
            } else{
                JSONObject map = arr.getJSONObject(0);
                Gson gson = new Gson();
                JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                return new ResponseEntity<>(transformOpenAIREToEOSCDatasource(jsonObj), HttpStatus.OK);
            }
        }
        throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", datasourceId));
    }

    public Datasource transformOpenAIREToEOSCDatasource(JsonElement openaireDatasource){
        Datasource datasource = new Datasource();
        String id = String.valueOf(openaireDatasource.getAsJsonObject().get("id")).replaceAll("\"", "");
        String name = String.valueOf(openaireDatasource.getAsJsonObject().get("officialname")).replaceAll("\"", "");
        datasource.setId(id);
        datasource.setName(name);
        return datasource;
    }

    public Paging<DatasourceBundle> getAllForAdminWithAuditStates(FacetFilter ff, MultiValueMap<String, Object> allRequestParams, Set<String> auditState, Authentication auth){
        List<DatasourceBundle> valid = new ArrayList<>();
        List<DatasourceBundle> notAudited = new ArrayList<>();
        List<DatasourceBundle> invalidAndUpdated = new ArrayList<>();
        List<DatasourceBundle> invalidAndNotUpdated = new ArrayList<>();

        int quantity = ff.getQuantity();
        int from = ff.getFrom();
        allRequestParams.remove("auditState");
        FacetFilter ff2 = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff2.setQuantity(1000);
        ff2.setFrom(0);
        Paging<DatasourceBundle> retPaging = getAllForAdmin(ff, auth);
        List<DatasourceBundle> allWithoutAuditFilterList =  getAllForAdmin(ff2, auth).getResults();
        List<DatasourceBundle> ret = new ArrayList<>();
        for (DatasourceBundle datasourceBundle : allWithoutAuditFilterList) {
            String auditVocStatus;
            try{
                auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(datasourceBundle.getLoggingInfo());
            } catch (NullPointerException e){ // datasourceBundle has null loggingInfo
                continue;
            }
            switch (auditVocStatus) {
                case "Valid and updated":
                case "Valid and not updated":
                    valid.add(datasourceBundle);
                    break;
                case "Not Audited":
                    notAudited.add(datasourceBundle);
                    break;
                case "Invalid and updated":
                    invalidAndUpdated.add(datasourceBundle);
                    break;
                case "Invalid and not updated":
                    invalidAndNotUpdated.add(datasourceBundle);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + auditVocStatus);
            }
        }
        for (String state : auditState) {
            switch (state) {
                case "Valid":
                    ret.addAll(valid);
                    break;
                case "Not Audited":
                    ret.addAll(notAudited);
                    break;
                case "Invalid and updated":
                    ret.addAll(invalidAndUpdated);
                    break;
                case "Invalid and not updated":
                    ret.addAll(invalidAndNotUpdated);
                    break;
                default:
                    throw new ValidationException(String.format("The audit state [%s] you have provided is wrong", state));
            }
        }
        if (!ret.isEmpty()) {
            List<DatasourceBundle> retWithCorrectQuantity = new ArrayList<>();
            if (from == 0){
                if (quantity <= ret.size()){
                    for (int i=from; i<=quantity-1; i++){
                        retWithCorrectQuantity.add(ret.get(i));
                    }
                } else{
                    retWithCorrectQuantity.addAll(ret);
                }
                retPaging.setTo(retWithCorrectQuantity.size());
            } else{
                boolean indexOutOfBound = false;
                if (quantity <= ret.size()){
                    for (int i=from; i<quantity+from; i++){
                        try{
                            retWithCorrectQuantity.add(ret.get(i));
                            if (quantity+from > ret.size()){
                                retPaging.setTo(ret.size());
                            } else{
                                retPaging.setTo(quantity+from);
                            }
                        } catch (IndexOutOfBoundsException e){
                            indexOutOfBound = true;
                            continue;
                        }
                    }
                    if (indexOutOfBound){
                        retPaging.setTo(ret.size());
                    }
                } else{
                    retWithCorrectQuantity.addAll(ret);
                    if (quantity+from > ret.size()){
                        retPaging.setTo(ret.size());
                    } else{
                        retPaging.setTo(quantity+from);
                    }
                }
            }
            retPaging.setFrom(from);
            retPaging.setResults(retWithCorrectQuantity);
            retPaging.setTotal(ret.size());
        } else{
            retPaging.setResults(ret);
            retPaging.setTotal(0);
            retPaging.setFrom(0);
            retPaging.setTo(0);
        }
        return retPaging;
    }
}
