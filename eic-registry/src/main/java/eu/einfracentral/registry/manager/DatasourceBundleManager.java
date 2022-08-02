package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.VocabularyService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@org.springframework.stereotype.Service
public class DatasourceBundleManager extends AbstractResourceBundleManager<DatasourceBundle> implements ResourceBundleService<DatasourceBundle> {

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

        // create ID if not exists
        if ((datasourceBundle.getDatasource().getId() == null) || ("".equals(datasourceBundle.getDatasource().getId()))) {
            String id = idCreator.createResourceId(datasourceBundle);
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
        facetFilter.addFilter("latest", true);
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
        ff.addFilter("latest", true);
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
        ff.addFilter("latest", true);
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
}
