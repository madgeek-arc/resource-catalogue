package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.VocabularyService;
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
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;
import static org.junit.Assert.assertTrue;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends AbstractServiceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private final ResourceManager<ProviderBundle> resourceManager;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final DataSource dataSource;
    private final String columnsOfInterest = "infra_service_id, name, abbreviation, resource_organisation, resource_providers, subcategories," +
            "scientific_subdomains, access_types, access_modes, language_availabilities, geographical_availabilities, resource_geographic_locations," +
            "trl, life_cycle_status, funding_body, funding_programs, tagline, open_source_technologies, order_type, catalogue_id";


    @Value("${project.name:}")
    private String projectName;

    @Autowired
    private VersionService versionService;

    @Autowired
    public InfraServiceManager(@Qualifier("providerManager") ResourceManager<ProviderBundle> resourceManager,
                               Random randomNumberGenerator, IdCreator idCreator,
                               @Lazy FieldValidator fieldValidator,
                               @Lazy SecurityService securityService,
                               @Lazy RegistrationMailService registrationMailService,
                               @Lazy VocabularyService vocabularyService, @Lazy DataSource dataSource) {
        super(InfraService.class);
        this.resourceManager = resourceManager; // for providers
        this.randomNumberGenerator = randomNumberGenerator;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.dataSource = dataSource;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService addService(InfraService infraService, Authentication auth) {
        // check if Provider is approved
        if (!resourceManager.get(infraService.getService().getResourceOrganisation()).getStatus().equals(vocabularyService.get("approved provider").getId())){
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    infraService.getService().getResourceOrganisation()));
        }

        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
            String id = idCreator.createServiceId(infraService.getService());
            infraService.getService().setId(id);
        }
        validate(infraService);
        validateEmailsAndPhoneNumbers(infraService);
        if (resourceManager.get(infraService.getService().getResourceOrganisation()).getTemplateStatus().equals(vocabularyService.get("approved template").getId())){
            infraService.setActive(true);
        } else{
            infraService.setActive(false);
        }
        infraService.setLatest(true);

        if (infraService.getMetadata() == null) {
            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        infraService.setLatestOnboardingInfo(loggingInfo);

        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));

        // resource status & extra loggingInfo for Approval
        ProviderBundle providerBundle = resourceManager.get(infraService.getService().getResourceOrganisation());
        if (providerBundle.getTemplateStatus().equals("approved template")){
            infraService.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            infraService.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            infraService.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        infraService.setLoggingInfo(loggingInfoList);

        logger.info("Adding Service: {}", infraService);
        InfraService ret;
        ret = super.add(infraService, auth);

        return ret;
    }

    //    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService updateService(InfraService infraService, String comment, Authentication auth) {
        InfraService ret;
        validate(infraService);
        validateEmailsAndPhoneNumbers(infraService);
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

        // set status
        infraService.setStatus(existingService.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingService.getStatus().equals(vocabularyService.get("rejected resource").getId())){
            ProviderBundle providerBundle = resourceManager.get(infraService.getService().getResourceOrganisation());
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())){
                infraService.setStatus(vocabularyService.get("pending resource").getId());
                infraService.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                resourceManager.update(providerBundle, auth);
            }
        }

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && infraService.getService().getVersion() == null) {
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

        if ((infraService.getService().getVersion() == null && existingService.getService().getVersion() == null)
                || infraService.getService().getVersion() != null
                && infraService.getService().getVersion().equals(existingService.getService().getVersion())) {
            infraService.setLatest(existingService.isLatest());
//            infraService.setStatus(existingService.getStatus());
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

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        String[] parts = id.split("\\.");
        String providerId = parts[0];
        InfraService infraService = null;
        List<InfraService> infraServices = getInfraServices(providerId, auth);
        for (InfraService service : infraServices){
            if (service.getService().getId().equals(id)){
                infraService = service;
            }
        }
        if (infraService == null){
            throw new ValidationException(String.format("The Resource with id '%s' does not exist", id));
        }
        infraService.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = resourceManager.get(infraService.getService().getResourceOrganisation());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        if (infraService.getLoggingInfo() != null) {
            loggingInfoList = infraService.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                infraService.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                infraService.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                infraService.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                infraService.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                infraService.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                infraService.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        logger.info("Verifying Resource: {}", infraService);
        resourceManager.update(resourceProvider, auth);
        return super.update(infraService, auth);
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
        List<ProviderBundle> providers = resourceManager.getAll(ff, null).getResults();
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

        if ((service.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                service.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !service.isActive()){
            throw new ValidationException(String.format("You cannot activate this Resource, because it's Inactive with status = [%s]", service.getStatus()));
        }

        ProviderBundle providerBundle = resourceManager.get(service.getService().getResourceOrganisation());
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
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
    public List<InfraService> getInfraServices(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public List<Service> getServices(String providerId, Authentication auth) {
        ProviderBundle providerBundle = resourceManager.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Services
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerId)) {
                return this.getAll(ff, auth).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
            }
        }
        // else return Provider's Services ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return this.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Resources of the specific Provider");
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    // Different that the one called on migration methods!
    @Override
    public InfraService getServiceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        List<InfraService> allProviderServices = getAll(ff, auth).getResults();
        for (InfraService infraService : allProviderServices){
            if (infraService.getStatus().equals(vocabularyService.get("pending resource").getId())){
                return infraService;
            }
        }
        return null;
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
        List<Service> services = getServices(providerId, null);
        Service featuredService = null;
        if (!services.isEmpty()) {
            featuredService = services.get(randomNumberGenerator.nextInt(services.size()));
        }
        return featuredService;
    }

//    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id) {
        InfraService infraService = new InfraService();
        try{
            infraService = get(id);
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
        } catch(ResourceNotFoundException e){
            logger.info(String.format("Resource with id [%s] not found", id));
        }
        return null;
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth){
        String providerId = resourceManager.get(get(resourceId).getService().getResourceOrganisation()).getId();
        String providerName = resourceManager.get(get(resourceId).getService().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public InfraService changeProvider(String resourceId, String newProviderId, String comment, Authentication auth){
        InfraService infraService = get(resourceId);
        ProviderBundle newProvider = resourceManager.get(newProviderId);
        ProviderBundle oldProvider =  resourceManager.get(infraService.getService().getResourceOrganisation());

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = infraService.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment == ""){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey());
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        infraService.setLoggingInfo(loggingInfoList);

        // update metadata
        Metadata metadata = infraService.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy( User.of(auth).getFullName());
        metadata.setTerms(null);
        infraService.setMetadata(metadata);

        // update id
        String initialId = infraService.getId();
        String[] parts = initialId.split("\\.");
        String serviceId = parts[1];
        String newResourceId = newProvider.getId()+"."+serviceId;
        infraService.setId(newResourceId);
        infraService.getService().setId(newResourceId);

        // update ResourceOrganisation
        infraService.getService().setResourceOrganisation(newProvider.getId());

        // add Resource, delete the old one
        add(infraService, auth);
        delete(get(resourceId));

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, infraService, auth);

        return infraService;
    }

    // TODO: First run with active/latest and no broke segment, second without active/latest and broke segment
    public Map<String, List<LoggingInfo>> migrateResourceHistory(Authentication authentication){
        Map<String, List<LoggingInfo>> allMigratedLoggingInfos = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
//        ff.addFilter("active", true);
//        ff.addFilter("latest", true);
        List<InfraService> allInfraServices = getAll(ff, securityService.getAdminAccess()).getResults();
        List<Resource> allResources;
        for (InfraService infraService : allInfraServices) {
            allResources = getResourcesWithServiceId(infraService.getService().getId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            boolean firstResource = true;
            boolean broke = false;
            for (Resource resource : allResources) {
                List<LoggingInfo> resourceHistory = new ArrayList<>();
                List<Version> versions = versionService.getVersionsByResource(resource.getId()); // get all updates of a specific Version of a specific Service
                versions.sort(Comparator.comparing(Version::getCreationDate));
                boolean firstVersion = true;
                for (Version version : versions) {
//                    broke = false;
                    // Save Version as Resource so we can deserialize it and get userFullName
                    Resource tempResource = resource;
                    tempResource.setPayload(version.getPayload());
                    InfraService tempService = deserialize(tempResource);
                    if (tempService.getLoggingInfo() != null && !tempService.getLoggingInfo().isEmpty()){
                        broke = true;
                        break;
                    }
                    LoggingInfo loggingInfo = new LoggingInfo();
                    if (firstResource && firstVersion) {
                        loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                        loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                        loggingInfo.setDate(String.valueOf(version.getCreationDate().getTime()));
                        if (tempService.getMetadata() != null && tempService.getMetadata().getRegisteredBy() != null){
                            if (tempService.getMetadata().getRegisteredBy().equalsIgnoreCase("System") || tempService.getMetadata().getRegisteredBy().equalsIgnoreCase("einfracentral")){
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
                            if (tempService.getMetadata().getModifiedBy().equalsIgnoreCase("System") || tempService.getMetadata().getModifiedBy().equalsIgnoreCase("einfracentral")){
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
                            if (tempService.getMetadata().getModifiedBy().equalsIgnoreCase("System") || tempService.getMetadata().getModifiedBy().equalsIgnoreCase("einfracentral")){
                                loggingInfo.setUserRole("system");
                            } else{
                                loggingInfo.setUserFullName(tempService.getMetadata().getModifiedBy());
                            }
                        }
                    }
                    resourceHistory.add(loggingInfo);
                }
                if (broke){
                    continue;
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
                try{
//                  super.update(infraService, securityService.getAdminAccess());
                } catch (ServiceException e){
                    continue;
                }
            }
        }
    return allMigratedLoggingInfos;
    }
    public Map<String, List<LoggingInfo>> migrateLatestResourceHistory(Authentication authentication){
        Map<String, List<LoggingInfo>> allMigratedLogginInfos = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<InfraService> allServices = getAll(ff, securityService.getAdminAccess()).getResults();
        for (InfraService infraService : allServices){
            boolean lastAuditFound = false;
            boolean lastUpdateFound = false;
            boolean lastOnboardFound = false;
            LoggingInfo lastUpdate = null;
            LoggingInfo lastAudit = null;
            LoggingInfo lastOnboard = null;
            List<LoggingInfo> loggingInfoList = null;
            try {
                loggingInfoList = getLoggingInfoHistory(infraService.getService().getId()).getResults();
            } catch (NullPointerException e){
                logger.info(e);
                continue;
            }
            for (LoggingInfo loggingInfo : loggingInfoList){
                if (loggingInfo.getType().equals(LoggingInfo.Types.UPDATE.getKey()) && !lastUpdateFound){
                    lastUpdate = loggingInfo;
                    lastUpdateFound = true;
                }
                if (loggingInfo.getType().equals(LoggingInfo.Types.AUDIT.getKey()) && !lastAuditFound){
                    lastAudit = loggingInfo;
                    lastAuditFound = true;
                }
                if (loggingInfo.getType().equals(LoggingInfo.Types.ONBOARD.getKey()) && !lastOnboardFound){
                    lastOnboard = loggingInfo;
                    lastOnboardFound = true;
                }
            }
            if (infraService.getLatestOnboardingInfo() == null){
                infraService.setLatestOnboardingInfo(lastOnboard);
            }
            if (infraService.getLatestUpdateInfo() == null){
                infraService.setLatestUpdateInfo(lastUpdate);
            }
            if (infraService.getLatestAuditInfo() == null){
                infraService.setLatestAuditInfo(lastAudit);
            }

            List<LoggingInfo> latestLoggings = new ArrayList<>();
            latestLoggings.add(lastOnboard);
            latestLoggings.add(lastUpdate);
            latestLoggings.add(lastAudit);
            logger.info(String.format("Resource's [%s] new Latest Onboard Info %s", infraService.getService().getName(), infraService.getLatestOnboardingInfo()));
            logger.info(String.format("Resource's [%s] new Latest Update Info %s", infraService.getService().getName(), infraService.getLatestUpdateInfo()));
            logger.info(String.format("Resource's [%s] new Latest Audit Info %s", infraService.getService().getName(), infraService.getLatestAuditInfo()));
//            super.update(infraService, securityService.getAdminAccess());
            allMigratedLogginInfos.put(infraService.getService().getId(), latestLoggings);
        }
        return allMigratedLogginInfos;
    }

    public void emailPhoneValidityCheck(){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<InfraService> allResources = getAll(ff, securityService.getAdminAccess()).getResults();
        List<InfraService> approvedActivedLatestResources = new ArrayList<>();
        for (InfraService infraService : allResources){
            logger.info(infraService.getId());
            if (infraService.getService().getResourceOrganisation().equals("catris")){
                continue;
            }
            if (infraService.isLatest() && infraService.getStatus().equals(vocabularyService.get("approved resource").getId())
                    && infraService.isActive()){
                approvedActivedLatestResources.add(infraService);
            }
        }
        logger.info(approvedActivedLatestResources.size());
        for (InfraService infraService : approvedActivedLatestResources){
            try{
                validateEmailsAndPhoneNumbers(infraService);
            } catch (ValidationException e){
                logger.info(String.format("Resource with id [%s])", infraService.getId()));
                logger.info(e);
            }
        }
    }

    public void validateEmailsAndPhoneNumbers(InfraService infraService){
        EmailValidator validator = EmailValidator.getInstance();
        Pattern pattern = Pattern.compile("^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$");
        // main contact email
        String mainContactEmail = infraService.getService().getMainContact().getEmail();
        if (!validator.isValid(mainContactEmail)) {
            throw new ValidationException(String.format("Email [%s] is not valid. Found in field Main Contact Email", mainContactEmail));
        }
        // main contact phone
        if (infraService.getService().getMainContact().getPhone() != null && !infraService.getService().getMainContact().getPhone().equals("")){
            String mainContactPhone = infraService.getService().getMainContact().getPhone();
            Matcher mainContactPhoneMatcher = pattern.matcher(mainContactPhone);
            try {
                assertTrue(mainContactPhoneMatcher.matches());
            } catch(AssertionError e){
                throw new ValidationException(String.format("The phone you provided [%s] is not valid. Found in field Main Contact Phone", mainContactPhone));
            }
        }
        // public contact
        for (ServicePublicContact servicePublicContact : infraService.getService().getPublicContacts()){
            // public contact email
            if (servicePublicContact.getEmail() != null && !servicePublicContact.getEmail().equals("")){
                String publicContactEmail = servicePublicContact.getEmail();
                if (!validator.isValid(publicContactEmail)) {
                    throw new ValidationException(String.format("Email [%s] is not valid. Found in field Public Contact Email", publicContactEmail));
                }
            }
            // public contact phone
            if (servicePublicContact.getPhone() != null && !servicePublicContact.getPhone().equals("")){
                String publicContactPhone = servicePublicContact.getPhone();
                Matcher publicContactPhoneMatcher = pattern.matcher(publicContactPhone);
                try {
                    assertTrue(publicContactPhoneMatcher.matches());
                } catch(AssertionError e){
                    throw new ValidationException(String.format("The phone you provided [%s] is not valid. Found in field Public Contact Phone", publicContactPhone));
                }
            }
        }
        // helpdesk email
        String helpdeskEmail = infraService.getService().getHelpdeskEmail();
        if (!validator.isValid(helpdeskEmail)) {
            throw new ValidationException(String.format("Email [%s] is not valid. Found in field Helpdesk Email", helpdeskEmail));
        }
        // security contact email
        String securityContactEmail = infraService.getService().getSecurityContactEmail();
        if (!validator.isValid(securityContactEmail)) {
            throw new ValidationException(String.format("Email [%s] is not valid. Found in field Security Contact Email", securityContactEmail));
        }
    }

    @Cacheable(value = CACHE_FEATURED)
    public List<Map<String, Object>> createQueryForResourceFilters (FacetFilter ff, String orderDirection, String orderField){
        String keyword = ff.getKeyword();
        Map<String, Object> order = ff.getOrderBy();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        List<String> allFilters = new ArrayList<>();

        String query;
        if (ff.getFilter().entrySet().isEmpty()){
            query = "SELECT infra_service_id FROM infra_service_view WHERE catalogue_id = 'eosc'";
        } else{
            query = "SELECT infra_service_id FROM infra_service_view WHERE";
        }

        boolean firstTime = true;
        boolean hasStatus = false;
        boolean hasCatalogueId = false;
        for (Map.Entry<String, Object> entry : ff.getFilter().entrySet()) {
            in.addValue(entry.getKey(), entry.getValue());
            // status
            if (entry.getKey().equals("status")) {
                hasStatus = true;
                if (firstTime) {
                    query += String.format(" (status=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    if (hasStatus && hasCatalogueId){
                        query += String.format(" AND (status=%s)", entry.getValue().toString());
                    }
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR status='");
                }
            }
            // catalogue_id
            if (entry.getKey().equals("catalogue_id")) {
                hasCatalogueId = true;
                if (firstTime) {
                    if (((LinkedHashSet) entry.getValue()).contains("all")){
                        query += String.format(" (catalogue_id LIKE '%%%%')");
                        firstTime = false;
                        continue;
                    } else{
                        query += String.format(" (catalogue_id=%s)", entry.getValue().toString());
                        firstTime = false;
                    }
                } else {
                    if ((hasStatus && hasCatalogueId)){
                        if (((LinkedHashSet) entry.getValue()).contains("all")){
                            query += String.format(" AND (catalogue_id LIKE '%%%%')");
                            continue;
                        } else{
                            query += String.format(" AND (catalogue_id=%s)", entry.getValue().toString());
                        }
                    }
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR catalogue_id='");
                }
            }
        }

        // keyword on search bar
        if (keyword != null && !keyword.equals("")){
            // replace apostrophes to avoid bad sql grammar
            if (keyword.contains("'")){
                keyword = keyword.replaceAll("'", "''");
            }
            if (firstTime){
                query += String.format(" WHERE upper(CONCAT(%s))", columnsOfInterest) + " like '%" + String.format("%s", keyword.toUpperCase()) + "%'";
            } else{
                query += String.format(" AND upper(CONCAT(%s))", columnsOfInterest) + " like '%" + String.format("%s", keyword.toUpperCase()) + "%'";
            }
        }

        // order/orderField
        if (orderField !=null && !orderField.equals("")){
            query += String.format(" ORDER BY %s", orderField);
        } else{
            query += " ORDER BY name";
        }
        if (orderDirection !=null && !orderDirection.equals("")){
            query += String.format(" %s", orderDirection);
        }

        query = query.replaceAll("\\[", "'").replaceAll("\\]","'");
        return namedParameterJdbcTemplate.queryForList(query, in);
    }

    @Cacheable(value = CACHE_FEATURED)
    public Paging<InfraService> createCorrectQuantityFacets(List<InfraService> infraServices, Paging<InfraService> infraServicePaging,
                                                              int quantity, int from){
        if (!infraServices.isEmpty()) {
            List<InfraService> retWithCorrectQuantity = new ArrayList<>();
            if (from == 0){
                if (quantity <= infraServices.size()){
                    for (int i=from; i<=quantity-1; i++){
                        retWithCorrectQuantity.add(infraServices.get(i));
                    }
                } else{
                    retWithCorrectQuantity.addAll(infraServices);
                }
                infraServicePaging.setTo(retWithCorrectQuantity.size());
            } else{
                boolean indexOutOfBound = false;
                if (quantity <= infraServices.size()){
                    for (int i=from; i<quantity+from; i++){
                        try{
                            retWithCorrectQuantity.add(infraServices.get(i));
                            if (quantity+from > infraServices.size()){
                                infraServicePaging.setTo(infraServices.size());
                            } else{
                                infraServicePaging.setTo(quantity+from);
                            }
                        } catch (IndexOutOfBoundsException e){
                            indexOutOfBound = true;
                            continue;
                        }
                    }
                    if (indexOutOfBound){
                        infraServicePaging.setTo(infraServices.size());
                    }
                } else{
                    retWithCorrectQuantity.addAll(infraServices);
                    if (quantity+from > infraServices.size()){
                        infraServicePaging.setTo(infraServices.size());
                    } else{
                        infraServicePaging.setTo(quantity+from);
                    }
                }
            }
            infraServicePaging.setFrom(from);
            infraServicePaging.setResults(retWithCorrectQuantity);
            infraServicePaging.setTotal(infraServices.size());
        } else{
            infraServicePaging.setResults(infraServices);
            infraServicePaging.setTotal(0);
            infraServicePaging.setFrom(0);
            infraServicePaging.setTo(0);
        }
        return infraServicePaging;
    }

    public Paging<InfraService> determineAuditState(Set<String> auditState, FacetFilter ff, int quantity, int from, List<InfraService> resources, Authentication auth) {
        List<InfraService> valid = new ArrayList<>();
        List<InfraService> notAudited = new ArrayList<>();
        List<InfraService> invalidAndUpdated = new ArrayList<>();
        List<InfraService> invalidAndNotUpdated = new ArrayList<>();

        Paging<InfraService> retPaging = getAll(ff, auth);
        List<InfraService> allWithoutAuditFilterList = new ArrayList<>();
        if (resources.isEmpty()){
            allWithoutAuditFilterList = getAll(ff, auth).getResults();
        } else{
            allWithoutAuditFilterList.addAll(resources);
        }
        List<InfraService> ret = new ArrayList<>();
        for (InfraService infraService : allWithoutAuditFilterList){
            String auditVocStatus;
            try{
                auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(infraService.getLoggingInfo());
            } catch (NullPointerException e){ // providerBundle has null loggingInfo
                continue;
            }
            switch (auditVocStatus){
                case "Valid and updated":
                case "Valid and not updated":
                    valid.add(infraService);
                    break;
                case "Not Audited":
                    notAudited.add(infraService);
                    break;
                case "Invalid and updated":
                    invalidAndUpdated.add(infraService);
                    break;
                case "Invalid and not updated":
                    invalidAndNotUpdated.add(infraService);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + auditVocStatus);
            }
        }
        for (String state : auditState){
            if (state.equals("Valid")){
                ret.addAll(valid);
            } else if (state.equals("Not Audited")){
                ret.addAll(notAudited);
            } else if (state.equals("Invalid and updated")){
                ret.addAll(invalidAndUpdated);
            } else if (state.equals("Invalid and not updated")) {
                ret.addAll(invalidAndNotUpdated);
            } else {
                throw new ValidationException(String.format("The audit state [%s] you have provided is wrong", state));
            }
        }
        return createCorrectQuantityFacets(ret, retPaging, quantity, from);
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
//    private InfraService migrate(InfraService service) throws MalformedURLException {
//        return service;
//    }

}
