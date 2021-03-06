package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;

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

    @Autowired
    public ProviderManager(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                           @Lazy SecurityService securityService,
                           @Lazy FieldValidator fieldValidator,
                           @Lazy RegistrationMailService registrationMailService,
                           IdCreator idCreator,
                           EventService eventService,
                           JmsTemplate jmsTopicTemplate,
                           VersionService versionService,
                           VocabularyService vocabularyService) {
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
    }


    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle provider, Authentication auth) {

        provider.setId(idCreator.createProviderId(provider.getProvider()));
        logger.trace("User '{}' is attempting to add a new Provider: {}", auth, provider);
        addAuthenticatedUser(provider.getProvider(), auth);
        validate(provider);
        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()) {
            validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }

        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add((loggingInfo));
        provider.setLoggingInfo(loggingInfoList);
        provider.setActive(false);
        provider.setStatus(vocabularyService.get("pending initial approval").getId());

        // latestOnboardingInfo
        provider.setLatestOnboardingInfo(loggingInfo);

        provider.getProvider().setParticipatingCountries(sortCountries(provider.getProvider().getParticipatingCountries()));

        ProviderBundle ret;
        ret = super.add(provider, null);
        logger.debug("Adding Provider: {}", provider);

        registrationMailService.sendEmailsToNewlyAddedAdmins(provider, null);

        jmsTopicTemplate.convertAndSend("provider.create", provider);

        return ret;
    }

    //    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle provider, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Provider with id '{}'", auth, provider);
        validate(provider);
        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()) {
            validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }
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

        Resource existing = whereID(provider.getId(), true);
        ProviderBundle ex = deserialize(existing);
        provider.setActive(ex.isActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Provider: {}", provider);

        // Send emails to newly added or deleted Admins
        adminDifferences(provider, ex);

        // send notification emails to Portal Admins
        if (provider.getLatestAuditInfo() != null && provider.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(provider.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(provider.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && provider.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(provider);
            }
        }

        jmsTopicTemplate.convertAndSend("provider.update", provider);

        return provider;
    }

    /**
     * Do not expose this method to users because it returns sensitive information about providers.
     *
     * @param id
     * @return
     */
    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public ProviderBundle get(String id) {
        ProviderBundle provider = super.get(id);
        if (provider == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find provider with id: %s", id));
        }
        return provider;
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public ProviderBundle get(String id, Authentication auth) {
        return get(id);
    }

    @Override
    public Paging<ResourceHistory> getHistory(String id) {
        Map<String, ResourceHistory> historyMap = new TreeMap<>();

        Resource resource = getResource(id);
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
    @Cacheable(value = CACHE_PROVIDERS)
    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
        List<ProviderBundle> userProviders = null;
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                    securityService.hasRole(auth, "ROLE_EPOT")) {
                return super.getAll(ff, auth);
            }
            // if user is not an admin, check if he is a provider
            userProviders = getMyServiceProviders(auth);
        }

        // retrieve providers
        Browsing<ProviderBundle> providers = super.getAll(ff, auth);

        if (userProviders != null) {
            // replace user providers having null users with complete provider entries
            userProviders.forEach(x -> {
                providers.getResults().removeIf(provider -> provider.getId().equals(x.getId()));
                providers.getResults().add(x);
            });
        }
        return providers;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(Authentication authentication, ProviderBundle provider) {
        logger.trace("User is attempting to delete the Provider with id '{}'", provider.getId());
        List<InfraService> services = infraServiceService.getInfraServices(provider.getId());
        services.forEach(s -> {
            try {
                infraServiceService.delete(s);
            } catch (ResourceNotFoundException e) {
                logger.error("Error deleting Service", e);
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
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle verifyProvider(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Provider state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Provider State!", status));
        }
        logger.trace("verifyProvider with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        ProviderBundle provider = get(id);
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
        InfraService serviceTemplate;
        switch (status) {
            case "approved":
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

                // update Service Template ProviderInfo
                serviceTemplate = updateInfraServiceLoggingInfo(id, LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey(), auth);
                try {
                    infraServiceService.update(serviceTemplate, auth);
                } catch (ResourceNotFoundException e) {
                    logger.error("Could not update service with id '{}' (verifyProvider)", serviceTemplate.getService().getId());
                }
                break;

            default:
                switch (status) {
                    case "rejected":
                        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                        loggingInfoList.add(loggingInfo);
                        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                        provider.setLoggingInfo(loggingInfoList);

                        // latestOnboardingInfo
                        provider.setLatestOnboardingInfo(loggingInfo);
                        break;
                    case "pending template submission":
                        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.VALIDATED.getKey());
                        loggingInfoList.add(loggingInfo);
                        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                        provider.setLoggingInfo(loggingInfoList);

                        // latestOnboardingInfo
                        provider.setLatestOnboardingInfo(loggingInfo);
                        break;
                    case "rejected template":
                        // update Service Template ProviderInfo
                        serviceTemplate = updateInfraServiceLoggingInfo(id, LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey(), auth);
                        try {
                            infraServiceService.update(serviceTemplate, auth);
                        } catch (ResourceNotFoundException e) {
                            logger.error("Could not update service with id '{}' (verifyProvider)", serviceTemplate.getService().getId());
                        }
                        break;
                    default:
                        break;
                }
        }
        if (active == null) {
            active = false;
        }

        if (active != null) {
            provider.setActive(active);
            if (!active) {
                if (!status.equals("pending template submission") && !status.equals("pending template approval")) {
                    loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.DEACTIVATED.getKey());
                    loggingInfoList.add(loggingInfo);
                    provider.setLoggingInfo(loggingInfoList);

                    // latestOnboardingInfo
                    provider.setLatestUpdateInfo(loggingInfo);
                }
                deactivateServices(provider.getId(), auth);
            } else {
                if (!status.equals("approved")) {
                    loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
                    loggingInfoList.add(loggingInfo);
                    provider.setLoggingInfo(loggingInfoList);

                    // latestOnboardingInfo
                    provider.setLatestUpdateInfo(loggingInfo);
                }
                activateServices(provider.getId(), auth);
            }
        }

        logger.info("Verifying Provider: {}", provider);
        return super.update(provider, auth);
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
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
    @Cacheable(value = CACHE_PROVIDERS)
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
        List<InfraService> services = infraServiceService.getInfraServices(providerId);
        logger.info("Activating all Services of the Provider with id: {}", providerId);
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
        List<InfraService> services = infraServiceService.getInfraServices(providerId);
        logger.info("Activating all Services of the Provider with id: {}", providerId);
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

        try {
            fieldValidator.validateFields(provider.getProvider());
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

    public void validateScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        for (ServiceProviderDomain providerScientificDomain : scientificDomains) {
            String[] parts = providerScientificDomain.getScientificSubdomain().split("-");
            String scientificDomain = "scientific_domain-" + parts[1];
            if (!providerScientificDomain.getScientificDomain().equals(scientificDomain)) {
                throw new ValidationException("Scientific Subdomain '" + providerScientificDomain.getScientificSubdomain() +
                        "' should have as Scientific Domain the value '" + scientificDomain + "'");
            }
        }
    }

    public void validateMerilScientificDomains(List<ProviderMerilDomain> merilScientificDomains) {
        for (ProviderMerilDomain providerMerilScientificDomain : merilScientificDomains) {
            String[] parts = providerMerilScientificDomain.getMerilScientificSubdomain().split("-");
            String merilScientificDomain = "provider_meril_scientific_domain-" + parts[1];
            if (!providerMerilScientificDomain.getMerilScientificDomain().equals(merilScientificDomain)) {
                throw new ValidationException("Meril Scientific Subdomain '" + providerMerilScientificDomain.getMerilScientificSubdomain() +
                        "' should have as Meril Scientific Domain the value '" + merilScientificDomain + "'");
            }
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
        ProviderBundle providerBundle = get(providerId);
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
        update(get(providerId), auth);
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
        ProviderBundle provider = get(providerId);
        for (User user : provider.getProvider().getUsers()) {
            if (user.getEmail().equalsIgnoreCase(User.of(auth).getEmail())) {
                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth));
            }
        }
    }

    public InfraService updateInfraServiceLoggingInfo(String providerId, String type,  String actionType, Authentication authentication) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        InfraService serviceTemplate = infraServiceService.getServiceTemplate(providerId);
        LoggingInfo loggingInfo;
        if (serviceTemplate.getLoggingInfo() != null) {
            loggingInfoList = serviceTemplate.getLoggingInfo();
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(authentication).getEmail(), User.of(authentication).getFullName(), securityService.getRoleName(authentication),
                    type, actionType);
            loggingInfoList.add(loggingInfo);
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(authentication).getEmail(), User.of(authentication).getFullName(), securityService.getRoleName(authentication),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(authentication).getEmail(), User.of(authentication).getFullName(), securityService.getRoleName(authentication), type, actionType);
            loggingInfoList.add(oldProviderRegistration);
            loggingInfoList.add(loggingInfo);
        }
        // latestOnboardingInfo
        serviceTemplate.setLatestOnboardingInfo(loggingInfo);

        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        serviceTemplate.setLoggingInfo(loggingInfoList);
        return serviceTemplate;
    }

    public List<String> sortCountries(List<String> countries) {
        Collections.sort(countries);
        return countries;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle auditProvider(String providerId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ProviderBundle provider = get(providerId);
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

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("status", "approved");
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
    public Paging<LoggingInfo> getLoggingInfoHistory(String id) {
        ProviderBundle providerBundle = get(id);
        if (providerBundle.getLoggingInfo() != null){
            List<LoggingInfo> loggingInfoList = providerBundle.getLoggingInfo();
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        }
        return null;
    }

    public Map<String, List<LoggingInfo>> migrateProviderHistory(Authentication auth){
        Map<String, List<LoggingInfo>> allMigratedLogginInfos = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ProviderBundle> allProviders = getAll(ff, auth).getResults();
        for (ProviderBundle providerBundle : allProviders){
            List<LoggingInfo> historyToLogging = new ArrayList<>();
            List<ResourceHistory> providerHistory = getHistory(providerBundle.getProvider().getId()).getResults();
            providerHistory.sort(Comparator.comparing(ResourceHistory::getModifiedAt));
            boolean earliestHistory = true;
            for (ResourceHistory history : providerHistory){
                LoggingInfo loggingInfo = new LoggingInfo();
                if (earliestHistory){
                    loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                    loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                    if (history.getRegisteredBy() != null){
                        loggingInfo.setUserFullName(history.getRegisteredBy());
                    }
                    if (history.getRegisteredAt() == null){
                        Resource resource = getResource(providerBundle.getId());
                        loggingInfo.setDate(String.valueOf(resource.getCreationDate().getTime()));
                    } else{
                        loggingInfo.setDate(history.getRegisteredAt());
                    }
                    earliestHistory = false;
                } else{
                    loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                    loggingInfo.setActionType(LoggingInfo.ActionType.UPDATED.getKey());
                    loggingInfo.setUserFullName(history.getModifiedBy());
                    loggingInfo.setDate(history.getModifiedAt());
                }
                historyToLogging.add(loggingInfo);
            }
            historyToLogging.sort(Comparator.comparing(LoggingInfo::getDate));
            if (providerBundle.getLoggingInfo() != null){
                List<LoggingInfo> loggingInfoList = providerBundle.getLoggingInfo();
                for (LoggingInfo loggingInfo : loggingInfoList){
                    // update initialization type
                    if (loggingInfo.getType().equals("initialization")){
                        loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                        loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                        loggingInfo.setDate(providerBundle.getMetadata().getRegisteredAt()); // we may need to go further to core creationDate
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
                    Instant firstHistoryInstant = Instant.ofEpochSecond(Long.parseLong(historyToLogging.get(0).getDate()));
                    Duration dif = Duration.between(firstHistoryInstant, loggingInstant);
                    long sec = dif.getSeconds();
                    if (sec > 20){ // if the difference < 20 secs, both lists contain the same items. If not (>20), concat them
                        for (LoggingInfo loggingFromHistory : historyToLogging) {
                            Instant historyInstant = Instant.ofEpochSecond(Long.parseLong(loggingFromHistory.getDate()));
                            Duration difference = Duration.between(historyInstant, loggingInstant);
                            long seconds = difference.getSeconds();
                            if (seconds > 20){
                                concatLoggingInfoList.add(loggingFromHistory);
                            } else{
                                concatLoggingInfoList.addAll(loggingInfoList);
                                providerBundle.setLoggingInfo(concatLoggingInfoList);
                                break;
                            }
                        }
                    }
                } // else it's on the Onboard state, so we keep the existing LoggingInfo
            } else{
                providerBundle.setLoggingInfo(historyToLogging);
            }
            logger.info(String.format("Provider's [%s] new Logging Info %s", providerBundle.getProvider().getName(), providerBundle.getLoggingInfo()));
//            super.update(providerBundle, auth);
            allMigratedLogginInfos.put(providerBundle.getProvider().getId(), providerBundle.getLoggingInfo());
        }
        return allMigratedLogginInfos;
    }
}
