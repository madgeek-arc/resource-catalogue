package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@org.springframework.stereotype.Service
public class ServiceBundleManager extends AbstractResourceBundleManager<ServiceBundle> implements ResourceBundleService<ServiceBundle> {

    private static final Logger logger = LogManager.getLogger(ServiceBundleManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public ServiceBundleManager(ProviderService<ProviderBundle, Authentication> providerService,
                                IdCreator idCreator, @Lazy SecurityService securityService,
                                @Lazy RegistrationMailService registrationMailService,
                                @Lazy VocabularyService vocabularyService,
                                CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(ServiceBundle.class);
        this.providerService = providerService; // for providers
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle addResource(ServiceBundle serviceBundle, Authentication auth) {
        return addResource(serviceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle addResource(ServiceBundle serviceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null) { // add catalogue provider
            serviceBundle.getService().setCatalogueId(catalogueName);
        } else { // add provider from external catalogue
            checkCatalogueIdConsistency(serviceBundle, catalogueId);
        }

        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getCatalogueId(), serviceBundle.getService().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' and catalogueId '%s' does not exist", serviceBundle.getService().getResourceOrganisation(), serviceBundle.getService().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    serviceBundle.getService().getResourceOrganisation()));
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")){
            throw new ValidationException(String.format("The Provider with id %s has already registered a Service Template.", providerBundle.getId()));
        }

        serviceBundle.setId(idCreator.createServiceId(serviceBundle));
        validate(serviceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        serviceBundle.setActive(active);

        // create new Metadata if not exists
        if (serviceBundle.getMetadata() == null) {
            serviceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfo);

        sortFields(serviceBundle);

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            serviceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            serviceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        serviceBundle.setLoggingInfo(loggingInfoList);

        logger.info("Adding Service: {}", serviceBundle);
        ServiceBundle ret;
        ret = super.add(serviceBundle, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle updateResource(ServiceBundle serviceBundle, String comment, Authentication auth) {
        return updateResource(serviceBundle, serviceBundle.getService().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #serviceBundle)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle updateResource(ServiceBundle serviceBundle, String catalogueId, String comment, Authentication auth) {
        ServiceBundle ret;

        if (catalogueId == null) {
            serviceBundle.getService().setCatalogueId(catalogueName);
        } else {
            checkCatalogueIdConsistency(serviceBundle, catalogueId);
        }

        logger.trace("User '{}' is attempting to update the Service with id '{}' of the Catalogue '{}'", auth, serviceBundle.getService().getId(), serviceBundle.getService().getCatalogueId());
        validate(serviceBundle);
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getCatalogueId(), serviceBundle.getService().getResourceOrganisation(), auth);
        ServiceBundle existingService;

        // if service version is empty set it null
        if ("".equals(serviceBundle.getService().getVersion())) {
            serviceBundle.getService().setVersion(null);
        }

        try { // try to find a Service with the same id
            existingService = get(serviceBundle.getService().getId(), serviceBundle.getService().getCatalogueId());
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Service with id [%s] on the [%s] Catalogue",
                    serviceBundle.getService().getId(), serviceBundle.getService().getCatalogueId()));
        }

        User user = User.of(auth);

        // update existing service serviceMetadata
        serviceBundle.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), user.getFullName()));
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        // update VS version update
        if (((serviceBundle.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                (serviceBundle.getService().getVersion().equals(existingService.getService().getVersion()))) {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
            if (existingService.getLoggingInfo() != null) {
                loggingInfoList = existingService.getLoggingInfo();
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
            } else {
                loggingInfoList.add(loggingInfo);
            }
        } else {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
            if (existingService.getLoggingInfo() != null) {
                loggingInfoList = existingService.getLoggingInfo();
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
            } else {
                loggingInfoList.add(loggingInfo);
            }
        }
        serviceBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        serviceBundle.setLatestUpdateInfo(loggingInfo);
        serviceBundle.setActive(existingService.isActive());
        sortFields(serviceBundle);

        // set status
        serviceBundle.setStatus(existingService.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingService.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
                serviceBundle.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, serviceBundle.getService().getCatalogueId(), auth);
            }
        }

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && serviceBundle.getService().getVersion() == null) {
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingService.getService().getCatalogueId().equals(serviceBundle.getService().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        ret = super.update(serviceBundle, auth);
        logger.info("Updating Service: {}", serviceBundle);

        // send notification emails to Portal Admins
        if (serviceBundle.getLatestAuditInfo() != null && serviceBundle.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(serviceBundle.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(serviceBundle.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && serviceBundle.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(serviceBundle);
            }
        }

        return ret;
    }

    public ServiceBundle getCatalogueResource(String catalogueId, String serviceId, Authentication auth) {
        ServiceBundle serviceBundle = get(serviceId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (serviceBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Service with id: %s", serviceId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
            throw new ValidationException(String.format("Service with id [%s] does not belong to the catalogue with id [%s]", serviceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            //TODO: userIsCatalogueAdmin -> transcationRollback error
            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceProviderAdmin(user, serviceId)) {
                return serviceBundle;
            }
        }
        // else return the Service ONLY if it is active
        if (serviceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return serviceBundle;
        }
        throw new ValidationException("You cannot view the specific Service");
    }

    @Override
    public void delete(ServiceBundle serviceBundle) {
        logger.info("Deleting Service: {}", serviceBundle);
        super.delete(serviceBundle);
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        String[] parts = id.split("\\.");
        String providerId = parts[0];
        ServiceBundle serviceBundle = null;
        List<ServiceBundle> serviceBundles = getResourceBundles(providerId, auth);
        for (ServiceBundle service : serviceBundles) {
            if (service.getService().getId().equals(id)) {
                serviceBundle = service;
            }
        }
        if (serviceBundle == null) {
            throw new ValidationException(String.format("The Resource with id '%s' does not exist", id));
        }
        serviceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(serviceBundle.getService().getResourceOrganisation());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        User user = User.of(auth);

        if (serviceBundle.getLoggingInfo() != null) {
            loggingInfoList = serviceBundle.getLoggingInfo();
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
                serviceBundle.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                serviceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                serviceBundle.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                serviceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        logger.info("Verifying Resource: {}", serviceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return super.update(serviceBundle, auth);
    }

    @Override
    public Paging<ServiceBundle> getInactiveResources() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        return getAll(ff, null);
    }

    @Override
    public ServiceBundle publish(String serviceId, boolean active, Authentication auth) {
        ServiceBundle service;
        String activeProvider = "";
        service = this.get(serviceId, catalogueName);

        if ((service.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                service.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !service.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Resource, because it's Inactive with status = [%s]", service.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(service.getService().getResourceOrganisation());
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = service.getService().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        service.setActive(active);

        User user = User.of(auth);
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        if (active) {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.ACTIVATED.getKey());
        } else {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DEACTIVATED.getKey());
        }
        if (service.getLoggingInfo() != null) {
            loggingInfoList = service.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
            loggingInfoList.add(loggingInfo);
        }
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        service.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        service.setLatestUpdateInfo(loggingInfo);

        this.update(service, auth);
        return service;
    }

    public ServiceBundle auditResource(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ServiceBundle service = get(serviceId, catalogueName);
        User user = User.of(auth);
        LoggingInfo loggingInfo; // TODO: extract method
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (service.getLoggingInfo() != null) {
            loggingInfoList = service.getLoggingInfo();
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(), actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        service.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        service.setLatestAuditInfo(loggingInfo);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForResourceAuditing(service);

        logger.info("Auditing Resource: {}", service);
        return super.update(service, auth);
    }

    public Paging<ServiceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("active", true);
        Browsing<ServiceBundle> serviceBrowsing = getAll(facetFilter, auth);
        Browsing<ServiceBundle> ret = serviceBrowsing;
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (ServiceBundle serviceBundle : serviceBrowsing.getResults()) {
            if (serviceBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(serviceBundle.getLatestAuditInfo().getDate()) > interval) {
                    int index = 0;
                    for (int i = 0; i < serviceBrowsing.getResults().size(); i++) {
                        if (serviceBrowsing.getResults().get(i).getService().getId().equals(serviceBundle.getService().getId())) {
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
    public List<ServiceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<ServiceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth);
    }

    @Override
    public List<Service> getResources(String providerId, Authentication auth) {
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
                return this.getAll(ff, auth).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
            }
        }
        // else return Provider's Services ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
            return this.getAll(ff, null).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Services of the specific Provider");
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Service> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
    }

    @Override
    public List<Service> getActiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("active", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
    }

    @Override
    public List<ServiceBundle> getInactiveResources(String providerId) {
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
        ServiceBundle serviceBundle = new ServiceBundle();
        try {
            serviceBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(serviceBundle.getService().getId(), serviceBundle.getService().getCatalogueId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources) {
                ServiceBundle service = deserialize(resource);
                if (service.getLoggingInfo() != null) {
                    loggingInfoList.addAll(service.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info(String.format("Resource with id [%s] not found", id));
        }
        return null;
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        String providerId = providerService.get(get(resourceId).getService().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getService().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public ServiceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        ServiceBundle serviceBundle = get(resourceId, catalogueName);
        // check Service's status
        if (!serviceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("You cannot move Service with id [%s] to another Provider as it" +
                    "is not yet Approved", serviceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId);
        ProviderBundle oldProvider = providerService.get(serviceBundle.getService().getResourceOrganisation());

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = serviceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment == "") {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        serviceBundle.setLoggingInfo(loggingInfoList);

        // update metadata
        Metadata metadata = serviceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        serviceBundle.setMetadata(metadata);

        // update id
        String initialId = serviceBundle.getId();
        String[] parts = initialId.split("\\.");
        String serviceId = parts[1];
        String newResourceId = newProvider.getId() + "." + serviceId;
        serviceBundle.setId(newResourceId);
        serviceBundle.getService().setId(newResourceId);

        // update ResourceOrganisation
        serviceBundle.getService().setResourceOrganisation(newProvider.getId());

        // add Resource, delete the old one
        add(serviceBundle, auth);
        delete(get(resourceId, catalogueName));

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, serviceBundle, auth);

        return serviceBundle;
    }

    public Paging<ServiceBundle> getAllForAdminWithAuditStates(FacetFilter ff, MultiValueMap<String, Object> allRequestParams, Set<String> auditState, Authentication auth){
        List<ServiceBundle> valid = new ArrayList<>();
        List<ServiceBundle> notAudited = new ArrayList<>();
        List<ServiceBundle> invalidAndUpdated = new ArrayList<>();
        List<ServiceBundle> invalidAndNotUpdated = new ArrayList<>();

        int quantity = ff.getQuantity();
        int from = ff.getFrom();
        allRequestParams.remove("auditState");
        FacetFilter ff2 = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff2.setQuantity(1000);
        ff2.setFrom(0);
        Paging<ServiceBundle> retPaging = getAllForAdmin(ff, auth);
        List<ServiceBundle> allWithoutAuditFilterList =  getAllForAdmin(ff2, auth).getResults();
        List<ServiceBundle> ret = new ArrayList<>();
        for (ServiceBundle serviceBundle : allWithoutAuditFilterList) {
            String auditVocStatus;
            try{
                auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(serviceBundle.getLoggingInfo());
            } catch (NullPointerException e){ // serviceBundle has null loggingInfo
                continue;
            }
            switch (auditVocStatus) {
                case "Valid and updated":
                case "Valid and not updated":
                    valid.add(serviceBundle);
                    break;
                case "Not Audited":
                    notAudited.add(serviceBundle);
                    break;
                case "Invalid and updated":
                    invalidAndUpdated.add(serviceBundle);
                    break;
                case "Invalid and not updated":
                    invalidAndNotUpdated.add(serviceBundle);
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
            List<ServiceBundle> retWithCorrectQuantity = new ArrayList<>();
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