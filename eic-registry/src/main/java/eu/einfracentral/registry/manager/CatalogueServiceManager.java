package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.SortUtils;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@Service("CatalogueServiceManager")
public class CatalogueServiceManager extends ResourceManager<InfraService> implements CatalogueServiceService<InfraService, Authentication> {

    private static final Logger logger = LogManager.getLogger(CatalogueServiceManager.class);

    private final ResourceManager<ProviderBundle> resourceManager;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final DataSource dataSource;
    private final InfraServiceService<InfraService, Authentication> infraServiceService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final AbstractServiceManager abstractServiceManager;

    @Value("${project.name:}")
    private String projectName;

    @Autowired
    private VersionService versionService;

    @Autowired
    public CatalogueServiceManager(@Qualifier("providerManager") ResourceManager<ProviderBundle> resourceManager,
                               Random randomNumberGenerator, IdCreator idCreator, @Lazy FieldValidator fieldValidator,
                               @Lazy SecurityService securityService, @Lazy RegistrationMailService registrationMailService,
                               @Lazy VocabularyService vocabularyService, @Lazy DataSource dataSource, InfraServiceService infraServiceService,
                               CatalogueService catalogueService, AbstractServiceManager abstractServiceManager) {
        super(InfraService.class);
        this.resourceManager = resourceManager; // for providers
        this.randomNumberGenerator = randomNumberGenerator;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.dataSource = dataSource;
        this.infraServiceService = infraServiceService;
        this.catalogueService = catalogueService;
        this.abstractServiceManager = abstractServiceManager;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Cacheable(value = CACHE_FEATURED)
    public InfraService getCatalogueService(String catalogueId, String serviceId, Authentication auth) {
        InfraService infraService = abstractServiceManager.get(serviceId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (infraService == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Service with id: %s", infraService));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!infraService.getService().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Service with id [%s] does not belong to the catalogue with id [%s]", serviceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            //TODO: userIsCatalogueAdmin -> transcationRollback error
            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsServiceProviderAdmin(user, serviceId)) {
                return infraService;
            }
        }
        // else return the Service ONLY if it is active
        if (infraService.getStatus().equals(vocabularyService.get("approved resource").getId())){
            return infraService;
        }
        throw new ValidationException("You cannot view the specific Service");
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService addCatalogueService(InfraService infraService, String catalogueId, Authentication auth) {
        checkCatalogueIdConsistency(infraService, catalogueId);
        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
            String id = idCreator.createCatalogueServiceId(infraService.getService());
            infraService.getService().setId(id);
        }
        infraServiceService.validate(infraService);
        infraServiceService.validateEmailsAndPhoneNumbers(infraService);
        infraService.setActive(true);
        infraService.setLatest(true);

        if (infraService.getMetadata() == null) {
            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        infraService.setLatestOnboardingInfo(loggingInfo);

        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));

        infraService.setStatus(vocabularyService.get("approved resource").getId());

        // LoggingInfo
        infraService.setLoggingInfo(loggingInfoList);

        logger.info("Adding Service: {}", infraService);
        InfraService ret;
        ret = super.add(infraService, auth);

        return ret;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService updateCatalogueService(InfraService infraService, String catalogueId, String comment, Authentication auth) {
        InfraService ret;
        logger.trace("User '{}' is attempting to update the Service with id '{}' of the Catalogue '{}'", auth, infraService.getService().getId(),
                infraService.getService().getCatalogueId());
        checkCatalogueIdConsistency(infraService, catalogueId);
        infraServiceService.validate(infraService);
        infraServiceService.validateEmailsAndPhoneNumbers(infraService);
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

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && infraService.getService().getVersion() == null) {
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")){
            if (!existingService.getService().getCatalogueId().equals(infraService.getService().getCatalogueId())){
                throw new ValidationException("You cannot change catalogueId");
            }
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

    public void checkCatalogueIdConsistency(InfraService infraService, String catalogueId){
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", catalogueId));
        }
        if (infraService.getService().getCatalogueId() == null || infraService.getService().getCatalogueId().equals("")){
            throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
        } else{
            if (!infraService.getService().getCatalogueId().equals(catalogueId)){
                throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
            }
        }
    }

}
