package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;

import javax.sql.DataSource;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateMerilScientificDomains;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;

@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends ResourceManager<ProviderBundle> implements ProviderService<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final SecurityService securityService;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final EventService eventService;
    private final JmsTemplate jmsTopicTemplate;
    private final RegistrationMailService registrationMailService;
    private final VersionService versionService;
    private final VocabularyService vocabularyService;
    private final DataSource dataSource;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    //TODO: maybe add description on DB and elastic too
    private final String columnsOfInterest = "provider_id, name, abbreviation, affiliations, tags, areas_of_activity, esfri_domains, meril_scientific_subdomains," +
        " networks, scientific_subdomains, societal_grand_challenges, structure_types, catalogue_id, hosting_legal_entity"; // variable with DB tables a keyword is been searched on

    private final SynchronizerService<Provider> synchronizerServiceProvider;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Value("${sync.enable}")
    private boolean enableSyncing;

    @Autowired
    public ProviderManager(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                           @Lazy SecurityService securityService,
                           @Lazy FieldValidator fieldValidator,
                           @Lazy RegistrationMailService registrationMailService,
                           IdCreator idCreator, EventService eventService,
                           JmsTemplate jmsTopicTemplate, VersionService versionService,
                           VocabularyService vocabularyService, DataSource dataSource,
                           SynchronizerService<Provider> synchronizerServiceProvider,
                           CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(ProviderBundle.class);
        this.infraServiceService = infraServiceService;
        this.securityService = securityService;
        this.fieldValidator = fieldValidator;
        this.idCreator = idCreator;
        this.eventService = eventService;
        this.registrationMailService = registrationMailService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.versionService = versionService;
        this.vocabularyService = vocabularyService;
        this.dataSource = dataSource;
        this.synchronizerServiceProvider = synchronizerServiceProvider;
        this.catalogueService = catalogueService;
    }


    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public boolean exists(ProviderBundle providerBundle) {
        return getResource(providerBundle.getProvider().getId(), providerBundle.getProvider().getCatalogueId()) != null;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle provider, Authentication authentication) {
        return add(provider, null, authentication);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle provider, String catalogueId, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Provider: {} on Catalogue: {}", auth, provider, catalogueId);

        provider = onboard(provider, catalogueId, auth);

        provider.setId(idCreator.createProviderId(provider.getProvider()));
        addAuthenticatedUser(provider.getProvider(), auth);
        validate(provider);
        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        sortFields(provider);

        ProviderBundle ret;
        ret = super.add(provider, null);
        logger.debug("Adding Provider: {} of Catalogue: {}", provider, catalogueId);

        registrationMailService.sendEmailsToNewlyAddedAdmins(provider, null);

        if (enableSyncing){
            synchronizerServiceProvider.syncAdd(provider.getProvider());
        }

        return ret;
    }

    //    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle provider, String comment, Authentication auth) {
        return update(provider, provider.getProvider().getCatalogueId(), comment, auth);
    }

    //    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle provider, String catalogueId, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Provider with id '{}' of the Catalogue '{}'", auth, provider, provider.getProvider().getCatalogueId());

        if (catalogueId == null) {
            provider.getProvider().setCatalogueId(catalogueName);
        } else {
            checkCatalogueIdConsistency(provider, catalogueId);
        }

        validate(provider);
        provider.setMetadata(Metadata.updateMetadata(provider.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey(), comment);
        if (provider.getLoggingInfo() != null) {
            loggingInfoList = provider.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        provider.getProvider().setParticipatingCountries(sortCountries(provider.getProvider().getParticipatingCountries()));
        provider.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        provider.setLatestUpdateInfo(loggingInfo);

        Resource existing = getResource(provider.getId(), provider.getProvider().getCatalogueId());
        ProviderBundle ex = deserialize(existing);

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN") && !ex.getProvider().getCatalogueId().equals(provider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot change catalogueId");
        }
        provider.setActive(ex.isActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Provider: {} of Catalogue: {}", provider, provider.getProvider().getCatalogueId());

        // Send emails to newly added or deleted Admins
        adminDifferences(provider, ex);

        // send notification emails to Portal Admins
        if (provider.getLatestAuditInfo() != null && provider.getLatestUpdateInfo() != null) {
            long latestAudit = Long.parseLong(provider.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(provider.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && provider.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(provider);
            }
        }

        if (enableSyncing) {
            synchronizerServiceProvider.syncUpdate(provider.getProvider());
        }

        return provider;
    }

    /**
     * Do not expose this method to users because it returns sensitive information about providers.
     *
     * @param id
     * @return
     */
    private ProviderBundle getWithCatalogue(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(String.format("Could not find provider with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    @Cacheable(value = CACHE_PROVIDERS, key = "#catalogueId+#providerId+(#auth!=null?#auth:'')")
    public ProviderBundle get(String catalogueId, String providerId, Authentication auth) {
        ProviderBundle providerBundle = getWithCatalogue(providerId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (providerBundle == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find provider with id: %s", providerId));
        }
        if (catalogueBundle == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", catalogueId));
        }
        if (!providerBundle.getProvider().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Provider with id [%s] does not belong to the catalogue with id [%s]", providerId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerId)) {
                return providerBundle;
            }
        }
        // else return the Provider ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return providerBundle;
        }
        throw new ValidationException("You cannot view the specific Provider");
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key = "#id+(#auth!=null?#auth:'')")
    public ProviderBundle get(String id, Authentication auth) {
        ProviderBundle providerBundle = get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, id)) {
                return providerBundle;
            }
        }
        // else return the Provider ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return providerBundle;
        }
        throw new ValidationException("You cannot view the specific Provider");
    }

    @Override
    public Paging<ResourceHistory> getHistory(String id, String catalogueId) {
        Map<String, ResourceHistory> historyMap = new TreeMap<>();

        Resource resource = getResource(id, catalogueId);
        List<Version> versions = versionService.getVersionsByResource(resource.getId());
        versions.sort((version, t1) -> {
            if (version.getCreationDate().getTime() < t1.getCreationDate().getTime()) {
                return -1;
            }
            return 1;
        });

        // create the first entry from the current resource
        ProviderBundle providerBundle;
        providerBundle = deserialize(resource);
        if (providerBundle != null && providerBundle.getMetadata() != null) {
            historyMap.put(providerBundle.getMetadata().getModifiedAt(), new ResourceHistory(providerBundle, resource.getId()));
        }

        // create version entries
        for (Version version : versions) {
            resource = (version.getResource() == null ? getResource(version.getParentId()) : version.getResource());
            resource.setPayload(version.getPayload());
            providerBundle = deserialize(resource);
            if (providerBundle != null) {
                try {
                    historyMap.putIfAbsent(providerBundle.getMetadata().getModifiedAt(), new ResourceHistory(providerBundle, version.getId()));
                } catch (NullPointerException e) {
                    logger.warn("Provider with id '{}' does not have Metadata", providerBundle.getId());
                }
            }
        }

        // sort list by modification date
        List<ResourceHistory> history = new ArrayList<>(historyMap.values());
        history.sort((resourceHistory, t1) -> {
            if (Long.parseLong(resourceHistory.getModifiedAt()) < Long.parseLong(t1.getModifiedAt())) {
                return 1;
            }
            return -1;
        });

        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key="#ff.hashCode()+(#auth!=null?#auth.hashCode():0)")
    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
        List<ProviderBundle> userProviders = null;
        List<ProviderBundle> retList = new ArrayList<>();

        // if user is ADMIN or EPOT return everything
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                    securityService.hasRole(auth, "ROLE_EPOT")) {
                return super.getAll(ff, auth);
            }
            // if user is PROVIDER ADMIN return all his Providers (rejected, pending) with their sensitive data (Users, MainContact) too
            User user = User.of(auth);
            Browsing<ProviderBundle> providers = super.getAll(ff, auth);
            for (ProviderBundle providerBundle : providers.getResults()){
                if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId()) ||
                securityService.userIsProviderAdmin(user, providerBundle.getId())) {
                    retList.add(providerBundle);
                }
            }
            providers.setResults(retList);
            providers.setTotal(retList.size());
            providers.setTo(retList.size());
            userProviders = getMyServiceProviders(auth);
            if (userProviders != null) {
                // replace user providers having null users with complete provider entries
                userProviders.forEach(x -> {
                    providers.getResults().removeIf(provider -> provider.getId().equals(x.getId()));
                    providers.getResults().add(x);
                });
            }
            return providers;
        }

        // else return ONLY approved Providers
        ff.addFilter("status", "approved provider");
        Browsing<ProviderBundle> providers = super.getAll(ff, auth);
        retList.addAll(providers.getResults());
        providers.setResults(retList);

        return providers;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(Authentication authentication, ProviderBundle provider) {
        logger.trace("User is attempting to delete the Provider with id '{}'", provider.getId());
        List<InfraService> services = infraServiceService.getInfraServices(provider.getId(), authentication);
        services.forEach(s -> {
            try {
                infraServiceService.delete(s);
            } catch (ResourceNotFoundException e) {
                logger.error("Error deleting Resource", e);
            }
        });
        logger.debug("Deleting Provider: {}", provider);

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(authentication).getEmail(), User.of(authentication).getEmail(), securityService.getRoleName(authentication),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DELETED.getKey());
        if (provider.getLoggingInfo() != null){
            loggingInfoList = provider.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else{
            loggingInfoList.add(loggingInfo);
        }
        provider.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        provider.setLatestUpdateInfo(loggingInfo);

        jmsTopicTemplate.convertAndSend("provider.delete", provider);

        super.delete(provider);
        registrationMailService.notifyProviderAdmins(provider);

        if (enableSyncing){
            synchronizerServiceProvider.syncDelete(provider.getProvider());
        }

    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle verifyProvider(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Provider state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Provider State!", status));
        }
        logger.trace("verifyProvider with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        ProviderBundle provider = get(catalogueName, id, auth);
        provider.setStatus(vocabularyService.get(status).getId());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        if (provider.getLoggingInfo() != null) {
            loggingInfoList = provider.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "approved provider":
                if (active == null) {
                    active = true;
                }
                provider.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                provider.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                provider.setLatestOnboardingInfo(loggingInfo);
                break;
            case "rejected provider":
                provider.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                provider.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                provider.setLatestOnboardingInfo(loggingInfo);
                break;
            default:
                break;
        }
        logger.info("Verifying Provider: {}", provider);
        return super.update(provider, auth);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle publish(String providerId, Boolean active, Authentication auth) {
        ProviderBundle provider = get(providerId, catalogueName);
        if ((provider.getStatus().equals(vocabularyService.get("pending provider").getId()) ||
                provider.getStatus().equals(vocabularyService.get("rejected provider").getId())) && !provider.isActive()){
            throw new ValidationException(String.format("You cannot activate this Provider, because it's Inactive with status = [%s]", provider.getStatus()));
        }
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        if (provider.getLoggingInfo() != null) {
            loggingInfoList = provider.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }

        if (active == null) {
            active = false;
        }
        if (active != null) {
            provider.setActive(active);
            if (!active) {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.DEACTIVATED.getKey());
                loggingInfoList.add(loggingInfo);
                provider.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                provider.setLatestUpdateInfo(loggingInfo);

                // deactivate Provider's Services
                deactivateServices(provider.getId(), auth);
                logger.info("Deactivating Provider: {}", provider);
            } else {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
                loggingInfoList.add(loggingInfo);
                provider.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                provider.setLatestUpdateInfo(loggingInfo);

                // activate Provider's Services
                activateServices(provider.getId(), auth);
                logger.info("Activating Provider: {}", provider);
            }
        }
        return super.update(provider, auth);
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key = "#email+(#auth!=null?#auth:'')")
    public List<ProviderBundle> getServiceProviders(String email, Authentication auth) {
        List<ProviderBundle> providers;
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        } else if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                securityService.hasRole(auth, "ROLE_EPOT")) {
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(maxQuantity);
            providers = super.getAll(ff, null).getResults();
        } else if (securityService.hasRole(auth, "ROLE_PROVIDER")) {
            providers = getMyServiceProviders(auth);
        } else {
            return new ArrayList<>();
        }
        return providers
                .stream()
                .map(p -> {
                    if (p.getProvider().getUsers() != null && p.getProvider().getUsers().stream().filter(Objects::nonNull).anyMatch(u -> {
                        if (u.getEmail() != null) {
                            return u.getEmail().equalsIgnoreCase(email);
                        }
                        return false;
                    })) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key = "(#auth!=null?#auth:'')")
    public List<ProviderBundle> getMyServiceProviders(Authentication auth) {
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        User user = User.of(auth);
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return super.getAll(ff, auth).getResults()
                .stream().map(p -> {
                    if (securityService.userIsProviderAdmin(user, p.getId())) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProviderBundle> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return getAll(ff, null).getResults();
    }

    public void activateServices(String providerId, Authentication auth) { // TODO: decide how to use service.status variable
        List<InfraService> services = infraServiceService.getInfraServices(providerId, auth);
        logger.info("Activating all Resources of the Provider with id: {}", providerId);
        for (InfraService service : services) {
            List<LoggingInfo> loggingInfoList;
            LoggingInfo loggingInfo;
            if (service.getLoggingInfo() != null){
                loggingInfoList = service.getLoggingInfo();
            } else{
                loggingInfoList = new ArrayList<>();
            }
            // distinction between system's (onboarding stage) and user's activation
            try {
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.ACTIVATED.getKey());
            } catch (InsufficientAuthenticationException e) {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
            }
            loggingInfoList.add(loggingInfo);

            // update Service
            service.setLoggingInfo(loggingInfoList);
            service.setLatestUpdateInfo(loggingInfo);
            service.setActive(true);

            try {
                logger.debug("Setting Service with name '{}' as active", service.getService().getName());
                infraServiceService.update(service, null);
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}", service.getService().getName());
            }
        }
    }

    public void deactivateServices(String providerId, Authentication auth) { // TODO: decide how to use service.status variable
        List<InfraService> services = infraServiceService.getInfraServices(providerId, auth);
        logger.info("Deactivating all Resources of the Provider with id: {}", providerId);
        for (InfraService service : services) {
            List<LoggingInfo> loggingInfoList;
            LoggingInfo loggingInfo;
            if (service.getLoggingInfo() != null){
                loggingInfoList = service.getLoggingInfo();
            } else{
                loggingInfoList = new ArrayList<>();
            }
            // distinction between system's (onboarding stage) and user's deactivation
            try {
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DEACTIVATED.getKey());
            } catch (InsufficientAuthenticationException e) {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.DEACTIVATED.getKey());
            }
            loggingInfoList.add(loggingInfo);

            // update Service
            service.setLoggingInfo(loggingInfoList);
            service.setLatestUpdateInfo(loggingInfo);
            service.setActive(false);

            try {
                logger.debug("Setting Service with name '{}' as active", service.getService().getName());
                infraServiceService.update(service, null);
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}", service.getService().getName());
            }
        }
    }

    @Override
    public ProviderBundle validate(ProviderBundle provider) {
        logger.debug("Validating Provider with id: {}", provider.getId());

        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()) {
            validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }
        try {
            fieldValidator.validate(provider);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        return provider;
    }

    @Override
    @CacheEvict(value = {CACHE_PROVIDERS, CACHE_SERVICE_EVENTS, CACHE_EVENTS}, allEntries = true)
    public void deleteUserInfo(Authentication authentication) {
        logger.trace("User '{}' is attempting to delete his User Info", authentication);
        String userEmail = ((OIDCAuthenticationToken) authentication).getUserInfo().getEmail();
        String userId = ((OIDCAuthenticationToken) authentication).getUserInfo().getSub();
        List<Event> allUserEvents = new ArrayList<>();
        allUserEvents.addAll(eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), authentication));
        allUserEvents.addAll(eventService.getUserEvents(Event.UserActionType.RATING.getKey(), authentication));
        List<ProviderBundle> allUserProviders = new ArrayList<>(getMyServiceProviders(authentication));
        for (ProviderBundle providerBundle : allUserProviders) {
            if (providerBundle.getProvider().getUsers().size() == 1) {
                throw new ValidationException(String.format("Your user info cannot be deleted, because you are the solely Admin of the Provider [%s]. " +
                        "You need to delete your Provider first or add more Admins.", providerBundle.getProvider().getName()));
            }
        }
        logger.info("Attempting to delete all user events");
        eventService.deleteEvents(allUserEvents);
        for (ProviderBundle providerBundle : allUserProviders) {
            List<User> updatedUsers = new ArrayList<>();
            for (User user : providerBundle.getProvider().getUsers()) {
                if (user.getId() != null && !"".equals(user.getId())) {
                    if (!user.getId().equals(userId)) {
                        updatedUsers.add(user);
                    }
                } else {
                    if (!user.getEmail().equals("") && !user.getEmail().equalsIgnoreCase(userEmail)) {
                        updatedUsers.add(user);
                    }
                }
            }
            providerBundle.getProvider().setUsers(updatedUsers);
            update(providerBundle, authentication);
        }
    }

    private void addAuthenticatedUser(Provider provider, Authentication auth) {
        List<User> users;
        User authUser = User.of(auth);
        users = provider.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
    }

    // For front-end use
    public boolean validateUrl(URL urlForValidation) {
        try {
            fieldValidator.validateUrl(null, urlForValidation);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth) {
        ProviderBundle providerBundle = get(catalogueName, providerId, auth);
        List<String> userList = new ArrayList<>();
        for (User user : providerBundle.getProvider().getUsers()) {
            userList.add(user.getEmail().toLowerCase());
        }
        if ((providerBundle.getMetadata().getTerms() == null || providerBundle.getMetadata().getTerms().isEmpty())) {
            if (userList.contains(User.of(auth).getEmail().toLowerCase())) {
                return false; //pop-up modal
            } else {
                return true; //no modal
            }
        }
        if (!providerBundle.getMetadata().getTerms().contains(User.of(auth).getEmail().toLowerCase()) && userList.contains(User.of(auth).getEmail().toLowerCase())) {
            return false; // pop-up modal
        }
        return true; // no modal
    }

    public void adminAcceptedTerms(String providerId, Authentication auth) {
        update(get(providerId), catalogueName, auth);
    }

    public void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
        List<String> existingAdmins = new ArrayList<>();
        List<String> newAdmins = new ArrayList<>();
        for (User user : existingProvider.getProvider().getUsers()) {
            existingAdmins.add(user.getEmail().toLowerCase());
        }
        for (User user : updatedProvider.getProvider().getUsers()) {
            newAdmins.add(user.getEmail().toLowerCase());
        }
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        if (!adminsAdded.isEmpty()) {
            registrationMailService.sendEmailsToNewlyAddedAdmins(updatedProvider, adminsAdded);
        }
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);
        if (!adminsDeleted.isEmpty()) {
            registrationMailService.sendEmailsToNewlyDeletedAdmins(existingProvider, adminsDeleted);
        }
    }

    public void requestProviderDeletion(String providerId, Authentication auth) {
        ProviderBundle provider = getWithCatalogue(providerId, catalogueName);
        for (User user : provider.getProvider().getUsers()) {
            if (user.getEmail().equalsIgnoreCase(User.of(auth).getEmail())) {
                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth));
            }
        }
    }

    public List<String> sortCountries(List<String> countries) {
        Collections.sort(countries);
        return countries;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle auditProvider(String providerId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ProviderBundle provider = get(providerId, catalogueName);
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (provider.getLoggingInfo() != null) {
            loggingInfoList = provider.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(),
                actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        provider.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        provider.setLatestAuditInfo(loggingInfo);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForProviderAuditing(provider);

        logger.info("Auditing Provider: {}", provider);
        return super.update(provider, auth);
    }

    public Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("status", "approved provider");
        Browsing<ProviderBundle> providerBrowsing = getAll(facetFilter, auth);
        List<ProviderBundle> providerList = getAll(facetFilter, auth).getResults();
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (ProviderBundle providerBundle : providerList) {
            if (providerBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(providerBundle.getLatestAuditInfo().getDate()) > interval) {
                    int index = 0;
                    for (int i=0; i<providerBrowsing.getResults().size(); i++){
                        if (providerBrowsing.getResults().get(i).getProvider().getId().equals(providerBundle.getProvider().getId())){
                            index = i;
                            break;
                        }
                    }
                    providerBrowsing.getResults().remove(index);
                }
            }
        }
        Collections.shuffle(providerBrowsing.getResults());
        for (int i = providerBrowsing.getResults().size() - 1; i > ff.getQuantity() - 1; i--) {
            providerBrowsing.getResults().remove(i);
        }
        providerBrowsing.setFrom(ff.getFrom());
        providerBrowsing.setTo(providerBrowsing.getResults().size());
        providerBrowsing.setTotal(providerBrowsing.getResults().size());
        return providerBrowsing;
    }

//    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        ProviderBundle providerBundle = getWithCatalogue(id, catalogueId);
        if (providerBundle.getLoggingInfo() != null){
            List<LoggingInfo> loggingInfoList = providerBundle.getLoggingInfo();
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        }
        return null;
    }

    public Paging<ProviderBundle> determineAuditState(Set<String> auditState, FacetFilter ff, List<ProviderBundle> providers, Authentication auth) {
        List<ProviderBundle> valid = new ArrayList<>();
        List<ProviderBundle> notAudited = new ArrayList<>();
        List<ProviderBundle> invalidAndUpdated = new ArrayList<>();
        List<ProviderBundle> invalidAndNotUpdated = new ArrayList<>();

        Paging<ProviderBundle> retPaging = getAll(ff, auth);
        List<ProviderBundle> allWithoutAuditFilterList = new ArrayList<>();
        if (providers.isEmpty()){
            allWithoutAuditFilterList = getAll(ff, auth).getResults();
        } else{
            allWithoutAuditFilterList.addAll(providers);
        }
        List<ProviderBundle> ret = new ArrayList<>();
        for (ProviderBundle providerBundle : allWithoutAuditFilterList){
            String auditVocStatus;
            try{
                auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(providerBundle.getLoggingInfo());
            } catch (NullPointerException e){ // providerBundle has null loggingInfo
                continue;
            }
            switch (auditVocStatus){
                case "Valid and updated":
                case "Valid and not updated":
                    valid.add(providerBundle);
                    break;
                case "Not Audited":
                    notAudited.add(providerBundle);
                    break;
                case "Invalid and updated":
                    invalidAndUpdated.add(providerBundle);
                    break;
                case "Invalid and not updated":
                    invalidAndNotUpdated.add(providerBundle);
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
        return createCorrectQuantityFacets(ret, retPaging, ff.getQuantity(), ff.getFrom());
    }

    public Paging<ProviderBundle> createCorrectQuantityFacets(List<ProviderBundle> providerBundle, Paging<ProviderBundle> providerBundlePaging,
                                                        int quantity, int from){
        if (!providerBundle.isEmpty()) {
            List<ProviderBundle> retWithCorrectQuantity = new ArrayList<>();
            if (from == 0){
                if (quantity <= providerBundle.size()){
                    for (int i=from; i<=quantity-1; i++){
                        retWithCorrectQuantity.add(providerBundle.get(i));
                    }
                } else{
                    retWithCorrectQuantity.addAll(providerBundle);
                }
                providerBundlePaging.setTo(retWithCorrectQuantity.size());
            } else{
                boolean indexOutOfBound = false;
                if (quantity <= providerBundle.size()){
                    for (int i=from; i<quantity+from; i++){
                        try{
                            retWithCorrectQuantity.add(providerBundle.get(i));
                            if (quantity+from > providerBundle.size()){
                                providerBundlePaging.setTo(providerBundle.size());
                            } else{
                                providerBundlePaging.setTo(quantity+from);
                            }
                        } catch (IndexOutOfBoundsException e){
                            indexOutOfBound = true;
                            continue;
                        }
                    }
                    if (indexOutOfBound){
                        providerBundlePaging.setTo(providerBundle.size());
                    }
                } else{
                    retWithCorrectQuantity.addAll(providerBundle);
                    if (quantity+from > providerBundle.size()){
                        providerBundlePaging.setTo(providerBundle.size());
                    } else{
                        providerBundlePaging.setTo(quantity+from);
                    }
                }
            }
            providerBundlePaging.setFrom(from);
            providerBundlePaging.setResults(retWithCorrectQuantity);
            providerBundlePaging.setTotal(providerBundle.size());
        } else{
            providerBundlePaging.setResults(providerBundle);
            providerBundlePaging.setTotal(0);
            providerBundlePaging.setFrom(0);
            providerBundlePaging.setTo(0);
        }
        return providerBundlePaging;
    }

    // TODO: refactor / delete?...
    public List<Map<String, Object>> createQueryForProviderFilters (FacetFilter ff, String orderDirection, String orderField){
        String keyword = ff.getKeyword();
        Map<String, Object> order = ff.getOrderBy();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        List<String> allFilters = new ArrayList<>();

        String query; // TODO: Replace with StringBuilder
        if (ff.getFilter().entrySet().isEmpty()){
            query = "SELECT provider_id FROM provider_view WHERE catalogue_id = 'eosc'";
        } else{
            query = "SELECT provider_id FROM provider_view WHERE";
        }

        boolean firstTime = true;
        boolean hasStatus = false;
        boolean hasTemplateStatus = false;
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
                    if (hasStatus && hasTemplateStatus){
                        query += String.format(" AND (status=%s)", entry.getValue().toString());
                    }
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR status='");
                }
            }
            // templateStatus
            if (entry.getKey().equals("templateStatus")) {
                hasTemplateStatus = true;
                if (firstTime) {
                    query += String.format(" (templateStatus=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    if (hasStatus && hasTemplateStatus){
                        query += String.format(" AND (templateStatus=%s)", entry.getValue().toString());
                    }
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR templateStatus='");
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
                    if ((hasStatus && hasCatalogueId) || (hasTemplateStatus && hasCatalogueId) || (hasStatus && hasTemplateStatus && hasCatalogueId)){
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
            query += String.format(" AND upper(CONCAT(%s))", columnsOfInterest) + " like '%" + String.format("%s", keyword.toUpperCase()) + "%'";
        }

        // order/orderField
        if (orderField != null && !orderField.equals("")){
            query += String.format(" ORDER BY %s", orderField);
        } else{
            query += " ORDER BY name";
        }
        if (orderDirection !=null && !orderDirection.equals("")){
            query += String.format(" %s", orderDirection);
        }

        query = query.replaceAll("\\[", "'").replaceAll("\\]","'");
        logger.debug(query);

        return namedParameterJdbcTemplate.queryForList(query, in);
    }

    public Resource getResource(String providerId, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("provider_id = \"%s\" AND catalogue_id = \"%s\"", providerId, catalogueId), resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    private void sortFields(ProviderBundle provider) {
        if (provider.getProvider().getParticipatingCountries() != null && !provider.getProvider().getParticipatingCountries().isEmpty()){
            provider.getProvider().setParticipatingCountries(sortCountries(provider.getProvider().getParticipatingCountries()));
        }
    }

    private ProviderBundle onboard(ProviderBundle provider, String catalogueId, Authentication auth) {
        // create LoggingInfo
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey()));
        provider.setLoggingInfo(loggingInfoList);
        if (catalogueId == null) {
            // set catalogueId = eosc
            provider.getProvider().setCatalogueId(catalogueName);
            provider.setActive(false);
            provider.setStatus(vocabularyService.get("pending provider").getId());
            provider.setTemplateStatus(vocabularyService.get("no template status").getId());
        } else {
            checkCatalogueIdConsistency(provider, catalogueId);
            provider.setActive(true);
            provider.setStatus(vocabularyService.get("approved provider").getId());
            provider.setTemplateStatus(vocabularyService.get("approved template").getId());
            loggingInfoList.add(LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey()));
        }

        // latestOnboardingInfo
        provider.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size()-1));

        return provider;
    }

    private void checkCatalogueIdConsistency(ProviderBundle provider, String catalogueId){
        catalogueService.existsOrElseThrow(catalogueId);
        if (provider.getProvider().getCatalogueId() == null || provider.getProvider().getCatalogueId().equals("")){
            throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
        } else{
            if (!provider.getProvider().getCatalogueId().equals(catalogueId)){
                throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
            }
        }
    }
}
