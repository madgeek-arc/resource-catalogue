package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.SortUtils;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends AbstractServiceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private final ProviderManager providerManager;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;


    @Value("${project.name:}")
    private String projectName;

    @Autowired
    private VersionService versionService;

    @Autowired
    public InfraServiceManager(ProviderManager providerManager, Random randomNumberGenerator, IdCreator idCreator,
                               @Lazy FieldValidator fieldValidator,
                               @Lazy SecurityService securityService,
                               @Lazy RegistrationMailService registrationMailService) {
        super(InfraService.class);
        this.providerManager = providerManager;
        this.randomNumberGenerator = randomNumberGenerator;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
    public InfraService addService(InfraService infraService, Authentication auth) {
        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
            String id = idCreator.createServiceId(infraService.getService());
            infraService.getService().setId(id);
        }
        validate(infraService);
        infraService.setActive(providerManager.get(infraService.getService().getResourceOrganisation()).isActive());

        infraService.setLatest(true);

        if (infraService.getMetadata() == null) {
            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        infraService.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        infraService.setLatestOnboardingInfo(loggingInfo);

        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));

        logger.info("Adding Service: {}", infraService);
        InfraService ret;
        ret = super.add(infraService, auth);

        return ret;
    }

    //    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #infraService)")
    public InfraService updateService(InfraService infraService, String comment, Authentication auth) {
        InfraService ret;
        validate(infraService);
        InfraService existingService;

        // if service version is empty set it null
        if ("".equals(infraService.getService().getVersion())) {
            infraService.getService().setVersion(null);
        }

        try { // try to find a service with the same id and version
            existingService = get(infraService.getService().getId(), infraService.getService().getVersion());
        } catch (ResourceNotFoundException e) {
            // if a service with version = infraService.getVersion() does not exist, get the latest service
            existingService = get(infraService.getService().getId());
        }
        if ("".equals(existingService.getService().getVersion())) {
            existingService.getService().setVersion(null);
        }

        // update existing service serviceMetadata
        infraService.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), User.of(auth).getFullName()));
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        // update VS version update
        if (((infraService.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                (infraService.getService().getVersion().equals(existingService.getService().getVersion()))){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
            if (existingService.getLoggingInfo() != null){
                loggingInfoList = existingService.getLoggingInfo();
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
            } else{
                loggingInfoList.add(loggingInfo);
            }
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
            loggingInfoList.add(loggingInfo);
        }
        infraService.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        infraService.setLatestUpdateInfo(loggingInfo);
        infraService.setActive(existingService.isActive());
        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && infraService.getService().getVersion() == null) {
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

        if ((infraService.getService().getVersion() == null && existingService.getService().getVersion() == null)
                || infraService.getService().getVersion() != null
                && infraService.getService().getVersion().equals(existingService.getService().getVersion())) {
            infraService.setLatest(existingService.isLatest());
            infraService.setStatus(existingService.getStatus());
            ret = super.update(infraService, auth);
            logger.info("Updating Service without version change: {}", infraService);
            logger.info("Service Version: {}", infraService.getService().getVersion());

        } else {
            // create new service and AFTERWARDS update the previous one (in case the new service cannot be created)
//                infraService.setStatus(); // TODO: enable this when services support the Status field

            // set previous service not latest
            existingService.setLatest(false);
            super.update(existingService, auth);
            logger.info("Updating Service with version change (super.update): {}", existingService);
            logger.info("Service Version: {}", existingService.getService().getVersion());

            // set new service as latest
            infraService.setLatest(true);
            ret = super.add(infraService, auth);
            logger.info("Updating Service with version change (super.add): {}", infraService);
        }

        // send notification emails to Portal Admins
        if (infraService.getLatestAuditInfo() != null && infraService.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(infraService.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(infraService.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && infraService.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(infraService);
            }
        }

        return ret;
    }

    @Override
    public void delete(InfraService infraService) {
        logger.info("Deleting Service: {}", infraService);
        super.delete(infraService);
    }

    @Override
    public Paging<InfraService> getInactiveServices() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        return getAll(ff, null);
    }

    @Override
    @Cacheable(CACHE_FEATURED)
    public List<Service> createFeaturedServices() {
        logger.info("Creating and caching 'featuredServices'");
        // TODO: return featured services (for now, it returns a random infraService for each provider)
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providers = providerManager.getAll(ff, null).getResults();
        List<Service> featuredServices = new ArrayList<>();
        List<Service> services;
        for (int i = 0; i < providers.size(); i++) {
            int rand = randomNumberGenerator.nextInt(providers.size());
            services = this.getActiveServices(providers.get(rand).getId());
            providers.remove(rand); // remove provider from list to avoid duplicate provider highlights
            if (!services.isEmpty()) {
                featuredServices.add(services.get(randomNumberGenerator.nextInt(services.size())));
            }
        }
        return featuredServices;
    }

    @Override
    public List<InfraService> eInfraCentralUpdate(InfraService service) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<InfraService> services = getAll(ff, null).getResults();
        List<InfraService> ret = new ArrayList<>();
        for (InfraService infraService : services) {
            try {
//                migrate(infraService); // use this to make custom changes
                ObjectUtils.merge(infraService, service); // use this to make bulk changes FIXME: this method does not work as expected
                validate(infraService);
                validateCategories(infraService.getService().getCategories());
                validateScientificDomains(infraService.getService().getScientificDomains());
                InfraService existingService = get(infraService.getService().getId());

                // update existing service serviceMetadata
                infraService.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), projectName));

                super.update(infraService, null);
                logger.info("Updating Service through merging: {}", infraService);
                ret.add(infraService);

            } catch (Exception e) {
                logger.error(e);
            }
        }
        return ret;
    }

    @Override
    public boolean validate(InfraService infraService) {
        Service service = infraService.getService();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Service with id: {}", service.getId());

        try {
            fieldValidator.validateFields(infraService.getService());
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        validateCategories(infraService.getService().getCategories());
        validateScientificDomains(infraService.getService().getScientificDomains());

        return true;
    }

    @Override
    public InfraService publish(String serviceId, String version, boolean active, Authentication auth) {
        InfraService service;
        String activeProvider = "";
        if (version == null || "".equals(version)) {
            service = this.get(serviceId);
        } else {
            service = this.get(serviceId, version);
        }

        ProviderBundle providerBundle = providerManager.get(service.getService().getResourceOrganisation());
        if (providerBundle.getStatus().equals("approved") && providerBundle.isActive()) {
            activeProvider = service.getService().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        service.setActive(active);
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        if (active){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.ACTIVATED.getKey());
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DEACTIVATED.getKey());
        }
        if (service.getLoggingInfo() != null){
            loggingInfoList = service.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        }
        else{
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
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

    public void validateCategories(List<ServiceCategory> categories) {
        for (ServiceCategory serviceCategory : categories) {
            String[] parts = serviceCategory.getSubcategory().split("-");
            String category = "category-" + parts[1] + "-" + parts[2];
            if (!serviceCategory.getCategory().equals(category)) {
                throw new ValidationException("Subcategory '" + serviceCategory.getSubcategory() + "' should have as Category the value '"
                        + category + "'");
            }
        }
    }

    public void validateScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        for (ServiceProviderDomain serviceScientificDomain : scientificDomains) {
            String[] parts = serviceScientificDomain.getScientificSubdomain().split("-");
            String scientificDomain = "scientific_domain-" + parts[1];
            if (!serviceScientificDomain.getScientificDomain().equals(scientificDomain)) {
                throw new ValidationException("Scientific Subdomain '" + serviceScientificDomain.getScientificSubdomain() +
                        "' should have as Scientific Domain the value '" + scientificDomain + "'");
            }
        }
    }


    public InfraService auditResource(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        InfraService service = get(serviceId);
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (service.getLoggingInfo() != null) {
            loggingInfoList = service.getLoggingInfo();
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(), actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        service.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        service.setLatestAuditInfo(loggingInfo);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForResourceAuditing(service);

        logger.info("Auditing Resource: {}", service);
        return super.update(service, auth);
    }

    public Paging<InfraService> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("active", true);
        facetFilter.addFilter("latest", true);
        List<InfraService> serviceList = getAll(facetFilter, auth).getResults();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("active", true);
        facetFilter.addFilter("latest", true);
        Browsing<InfraService> serviceBrowsing = getAll(facetFilter, auth);
        Browsing<InfraService> ret = serviceBrowsing;
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (InfraService infraService : serviceList) {
            if (infraService.getLatestAuditInfo() != null) {
                if (Long.parseLong(infraService.getLatestAuditInfo().getDate()) > interval) {
                    int index = 0;
                    for (int i=0; i<serviceBrowsing.getResults().size(); i++){
                        if (serviceBrowsing.getResults().get(i).getService().getId().equals(infraService.getService().getId())){
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
    public List<InfraService> getInfraServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults();
    }

    @Override
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    @Override
    public InfraService getServiceTemplate(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.setQuantity(1);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("registeredAt", "asc"));
        return this.getAll(ff, null).getResults().get(0);
    }

    @Override
    public List<Service> getActiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    @Override
    public List<InfraService> getInactiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults();
    }

    //Gets random Services to be featured at the Carousel
    @Override
    public Service getFeaturedService(String providerId) {
        // TODO: change this method
        List<Service> services = getServices(providerId);
        Service featuredService = null;
        if (!services.isEmpty()) {
            featuredService = services.get(randomNumberGenerator.nextInt(services.size()));
        }
        return featuredService;
    }

//    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id) {
        InfraService infraService = get(id);
        List<Resource> allResources = getResourcesWithServiceId(infraService.getService().getId()); // get all versions of a specific Service
        allResources.sort(Comparator.comparing((Resource::getCreationDate)));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        for (Resource resource : allResources){
            InfraService service = deserialize(resource);
            if (service.getLoggingInfo() != null){
                loggingInfoList.addAll(service.getLoggingInfo());
            }
        }
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
    }

    // TODO: After migrating for active/latest, migrate for inactive/non-latest too (if infraService.loggingInfo == null/empty)
    public Map<String, List<LoggingInfo>> migrateResourceHistory(Authentication auth){
        Map<String, List<LoggingInfo>> allMigratedLoggingInfos = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        List<InfraService> allInfraServices = getAll(ff, auth).getResults();
        List<Resource> allResources;
        for (InfraService infraService : allInfraServices) {
            allResources = getResourcesWithServiceId(infraService.getService().getId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            boolean firstResource = true;
            for (Resource resource : allResources) {
                List<LoggingInfo> resourceHistory = new ArrayList<>();
                List<Version> versions = versionService.getVersionsByResource(resource.getId()); // get all updates of a specific Version of a specific Service
                versions.sort(Comparator.comparing(Version::getCreationDate));
                boolean firstVersion = true;
                for (Version version : versions) {
                    // Save Version as Resource so we can deserialize it and get userFullName
                    Resource tempResource = resource;
                    tempResource.setPayload(version.getPayload());
                    InfraService tempService = deserialize(tempResource);
                    LoggingInfo loggingInfo = new LoggingInfo();
                    if (firstResource && firstVersion) {
                        loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                        loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                        loggingInfo.setDate(String.valueOf(version.getCreationDate().getTime()));
                        if (tempService.getMetadata() != null && tempService.getMetadata().getRegisteredBy() != null){
                            if (tempService.getMetadata().getRegisteredBy().equalsIgnoreCase("System")){
                                loggingInfo.setUserRole("system");
                            } else{
                                loggingInfo.setUserFullName(tempService.getMetadata().getRegisteredBy());
                            }
                        }
                        firstResource = false;
                        firstVersion = false;
                    } else if (!firstResource && firstVersion) {
                        loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                        loggingInfo.setActionType(LoggingInfo.ActionType.UPDATED_VERSION.getKey());
                        loggingInfo.setDate(String.valueOf(version.getCreationDate().getTime()));
                        if (tempService.getMetadata() != null && tempService.getMetadata().getModifiedBy() != null){
                            if (tempService.getMetadata().getModifiedBy().equalsIgnoreCase("System")){
                                loggingInfo.setUserRole("system");
                            } else{
                                loggingInfo.setUserFullName(tempService.getMetadata().getModifiedBy());
                            }
                        }
                        firstVersion = false;
                    } else {
                        loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                        loggingInfo.setActionType(LoggingInfo.ActionType.UPDATED.getKey());
                        loggingInfo.setDate(String.valueOf(version.getCreationDate().getTime()));
                        if (tempService.getMetadata() != null && tempService.getMetadata().getModifiedBy() != null){
                            if (tempService.getMetadata().getModifiedBy().equalsIgnoreCase("System")){
                                loggingInfo.setUserRole("system");
                            } else{
                                loggingInfo.setUserFullName(tempService.getMetadata().getModifiedBy());
                            }
                        }
                    }
                    resourceHistory.add(loggingInfo);
                }
                resourceHistory.sort(Comparator.comparing(LoggingInfo::getDate));

                InfraService service = deserialize(resource);

                if (service.getLoggingInfo() != null) {
                    List<LoggingInfo> loggingInfoList = service.getLoggingInfo();
                    for (LoggingInfo loggingInfo : loggingInfoList) {
                        // update initialization type
                        if (loggingInfo.getType().equals("initialization")) {
                            loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                            loggingInfo.setDate(service.getMetadata().getRegisteredAt()); // we may need to go further to core creationDate
                        }
                        // migrate all the other states
                        if (loggingInfo.getType().equals("registered")){
                            loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                        } else if (loggingInfo.getType().equals("updated")){
                            loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.UPDATED.getKey());
                        } else if (loggingInfo.getType().equals("deleted")){
                            loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.DELETED.getKey());
                        } else if (loggingInfo.getType().equals("activated")){
                            loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.ACTIVATED.getKey());
                        } else if (loggingInfo.getType().equals("deactivated")){
                            loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.DEACTIVATED.getKey());
                        } else if (loggingInfo.getType().equals("approved")){
                            loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.APPROVED.getKey());
                        } else if (loggingInfo.getType().equals("validated")){
                            loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.VALIDATED.getKey());
                        } else if (loggingInfo.getType().equals("rejected")){
                            loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                            loggingInfo.setActionType(LoggingInfo.ActionType.REJECTED.getKey());
                        } else if (loggingInfo.getType().equals("audited")){
                            loggingInfo.setType(LoggingInfo.Types.AUDIT.getKey());
                        }
                    }
                    List<LoggingInfo> concatLoggingInfoList = new ArrayList<>();
                    if (loggingInfoList.get(0).getType().equals(LoggingInfo.Types.UPDATE.getKey()) || loggingInfoList.get(0).getType().equals(LoggingInfo.Types.AUDIT.getKey())) {
                        Instant loggingInstant = Instant.ofEpochSecond(Long.parseLong(loggingInfoList.get(0).getDate()));
                        Instant firstHistoryInstant = Instant.ofEpochSecond(Long.parseLong(resourceHistory.get(0).getDate()));
                        Duration dif = Duration.between(firstHistoryInstant, loggingInstant);
                        long sec = dif.getSeconds();
                        if (sec > 20){ // if the difference < 20 secs, both lists contain the same items. If not (>20), concat them
                            for (LoggingInfo loggingFromHistory : resourceHistory) {
                                Instant historyInstant = Instant.ofEpochSecond(Long.parseLong(loggingFromHistory.getDate()));
                                Duration difference = Duration.between(historyInstant, loggingInstant);
                                long seconds = difference.getSeconds();
                                if (seconds > 20){
                                    concatLoggingInfoList.add(loggingFromHistory);
                                } else{
                                    concatLoggingInfoList.addAll(loggingInfoList);
                                    service.setLoggingInfo(concatLoggingInfoList);
                                    break;
                                }
                            }
                        }
                    } // else it's on the Onboard state, so we keep the existing LoggingInfo
                } else {
                    service.setLoggingInfo(resourceHistory);
                }
                if (service.getService().getVersion() == null){
                    allMigratedLoggingInfos.put(service.getService().getId()+" with null version", service.getLoggingInfo());
                } else{
                    allMigratedLoggingInfos.put(service.getService().getId()+" with version "+service.getService().getVersion(), service.getLoggingInfo());
                }
                logger.info(String.format("Resource's [%s] new Logging Info %s", service.getService().getName(), service.getLoggingInfo()));
                super.update(service, auth);
            }
        }
    return allMigratedLoggingInfos;
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
//    private InfraService migrate(InfraService service) throws MalformedURLException {
//        return service;
//    }

}
