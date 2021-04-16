package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
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
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;

import javax.validation.constraints.Null;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;

@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends ResourceManager<ProviderBundle> implements ProviderService<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final SecurityService securityService;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final EventService eventService;
    private final JmsTemplate jmsTopicTemplate;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;

    @Autowired
    public ProviderManager(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                           @Lazy SecurityService securityService, Random randomNumberGenerator,
                           @Lazy FieldValidator fieldValidator, @Lazy RegistrationMailService registrationMailService,
                           IdCreator idCreator, EventService eventService, JmsTemplate jmsTopicTemplate,
                           VocabularyService vocabularyService) {
        super(ProviderBundle.class);
        this.infraServiceService = infraServiceService;
        this.securityService = securityService;
        this.randomNumberGenerator = randomNumberGenerator;
        this.fieldValidator = fieldValidator;
        this.idCreator = idCreator;
        this.eventService = eventService;
        this.registrationMailService = registrationMailService;
        this.jmsTopicTemplate = jmsTopicTemplate;
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
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()){
            validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }

        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfo(User.of(auth).getEmail(), determineRole(auth));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add((loggingInfo));
        provider.setLoggingInfo(loggingInfoList);
        provider.setActive(false);
        provider.setStatus(vocabularyService.get("pending initial approval").getId());

        ProviderBundle ret;
        ret = super.add(provider, null);
        logger.debug("Adding Provider: {}", provider);

        registrationMailService.sendEmailsToNewlyAddedAdmins(provider, null);

        jmsTopicTemplate.convertAndSend("provider.create", provider);

        return ret;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle provider, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Provider with id '{}'", auth, provider);
        validate(provider);
        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()){
            validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }
        provider.setMetadata(Metadata.updateMetadata(provider.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList;
        if (provider.getLoggingInfo() != null){
            loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.UPDATED.getKey());
            loggingInfoList = provider.getLoggingInfo();
            loggingInfoList.add((loggingInfo));
        } else{
            loggingInfo = LoggingInfo.createLoggingInfo(User.of(auth).getEmail(), determineRole(auth));
            loggingInfo.setType(LoggingInfo.Types.UPDATED.getKey());
            loggingInfoList = new ArrayList<>();
            loggingInfoList.add((loggingInfo));
        }
        provider.setLoggingInfo(loggingInfoList);
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
    @Cacheable(value = CACHE_PROVIDERS)
    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
        List<ProviderBundle> userProviders = null;
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN")) {
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
        List<InfraService> services = this.getInfraServices(provider.getId());
        services.forEach(s -> {
            try {
                infraServiceService.delete(s);
            } catch (ResourceNotFoundException e) {
                logger.error("Error deleting Service", e);
            }
        });
        logger.debug("Deleting Provider: {}", provider);

        List<LoggingInfo> loggingInfoList = provider.getLoggingInfo();
        LoggingInfo loggingInfo = new LoggingInfo();
        loggingInfo.setUserRole(determineRole(authentication));
        loggingInfo.setType(LoggingInfo.Types.DELETED.getKey());
        loggingInfo.setUserEmail(User.of(authentication).getEmail());
        loggingInfoList.add(loggingInfo);
        provider.setLoggingInfo(loggingInfoList);

        jmsTopicTemplate.convertAndSend("provider.delete", provider);

        super.delete(provider);
        registrationMailService.notifyProviderAdmins(provider);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle verifyProvider(String id, String status, Boolean active, Authentication auth) {
        try {
            vocabularyService.get(status);
        } catch(ResourceException e){
            throw new ResourceException(String.format("Vocabulary %s does not exist!", status), HttpStatus.NOT_FOUND);
        }
        if (!vocabularyService.get(status).getType().equals("Provider state")){
            throw new ValidationException(String.format("Vocabulary %s does not consist a Provider State!", status));
        }
        logger.trace("verifyProvider with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        ProviderBundle provider = get(id);
        provider.setStatus(vocabularyService.get(status).getId());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (provider.getLoggingInfo() != null){
            loggingInfoList = provider.getLoggingInfo();
        } else{
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoForExistingEntry(User.of(auth).getEmail(), determineRole(auth));
            loggingInfoList.add(oldProviderRegistration);
        }
        InfraService serviceTemplate;
        switch (status) {
            case "approved":
                if (active == null) {
                    active = true;
                }
                provider.setActive(active);
                loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.APPROVED.getKey());
                loggingInfoList.add((loggingInfo));
                provider.setLoggingInfo(loggingInfoList);

                // update Service Template ProviderInfo
                serviceTemplate = updateInfraServiceLoggingInfo(id, LoggingInfo.Types.APPROVED.getKey(), auth);
                try {
                    infraServiceService.update(serviceTemplate, auth);
                } catch (ResourceNotFoundException e) {
                    logger.error("Could not update service with id '{}' (verifyProvider)", serviceTemplate.getService().getId());
                }
                break;

            default:
                switch(status) {
                    case "rejected":
                        loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.REJECTED.getKey());
                        loggingInfoList.add((loggingInfo));
                        provider.setLoggingInfo(loggingInfoList);
                        break;
                    case "pending template submission":
                        loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.VALIDATED.getKey());
                        loggingInfoList.add((loggingInfo));
                        provider.setLoggingInfo(loggingInfoList);
                        break;
                    case "rejected template":
                        // update Service Template ProviderInfo
                        serviceTemplate = updateInfraServiceLoggingInfo(id, LoggingInfo.Types.REJECTED.getKey(), auth);
                        try {
                            infraServiceService.update(serviceTemplate, auth);
                        } catch (ResourceNotFoundException e) {
                            logger.error("Could not update service with id '{}' (verifyProvider)", serviceTemplate.getService().getId());
                        }
                        break;
                    default:
                        break;
                }
                provider.setActive(false);
        }

        if (active != null) {
            provider.setActive(active);
            if (!active) {
                if (!status.equals("pending template submission") && !status.equals("pending template approval")){
                    loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.DEACTIVATED.getKey());
                    loggingInfoList.add((loggingInfo));
                    provider.setLoggingInfo(loggingInfoList);
                }
                deactivateServices(provider.getId(), auth);
            } else {
                if (!status.equals("approved")){
                    loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.ACTIVATED.getKey());
                    loggingInfoList.add((loggingInfo));
                    provider.setLoggingInfo(loggingInfoList);
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
        } else if (securityService.hasRole(auth, "ROLE_ADMIN")) {
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(10000);
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
                            return u.getEmail().equals(email);
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
        ff.setQuantity(10000);
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
    public List<InfraService> getInfraServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults();
    }

    @Override
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("latest", true);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    @Override
    public List<Service> getActiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
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

    @Override
    public List<ProviderBundle> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return getAll(ff, null).getResults();
    }

    @Override
    public List<InfraService> getInactiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults();
    }

    public void activateServices(String providerId, Authentication auth) { // TODO: decide how to use service.status variable
        List<InfraService> services = this.getInfraServices(providerId);
        logger.info("Activating all Services of the Provider with id: {}", providerId);
        for (InfraService service : services) {
//            service.setActive(service.getStatus() == null || service.getStatus().equals("true"));
//            service.setStatus(null);
            service.setActive(true);
            // update LoggingInfo
            List<LoggingInfo> loggingInfoList = service.getLoggingInfo();
            LoggingInfo loggingInfo;
            try{
                loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.ACTIVATED.getKey());
            } catch(InsufficientAuthenticationException e){
                loggingInfo = LoggingInfo.updateLoggingInfo(LoggingInfo.Types.ACTIVATED.getKey());
            }
            loggingInfoList.add(loggingInfo);
            service.setLoggingInfo(loggingInfoList);
            try {
                logger.debug("Setting Service with name '{}' as active", service.getService().getName());
                infraServiceService.update(service, null);
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}", service.getService().getName());
            }
        }
    }

    public void deactivateServices(String providerId, Authentication auth) { // TODO: decide how to use service.status variable
        List<InfraService> services = this.getInfraServices(providerId);
        logger.info("Deactivating all Services of the Provider with id: {}", providerId);
        for (InfraService service : services) {
//            service.setStatus(service.isActive() != null ? service.isActive().toString() : "true");
//            service.setStatus(null);
            service.setActive(false);
            List<LoggingInfo> loggingInfoList = service.getLoggingInfo();
            LoggingInfo loggingInfo;
            try{
                loggingInfo = LoggingInfo.updateLoggingInfo(User.of(auth).getEmail(), determineRole(auth), LoggingInfo.Types.DEACTIVATED.getKey());
            } catch(InsufficientAuthenticationException e){
                loggingInfo = LoggingInfo.updateLoggingInfo(LoggingInfo.Types.DEACTIVATED.getKey());
            }
            loggingInfoList.add(loggingInfo);
            service.setLoggingInfo(loggingInfoList);
            try {
                logger.debug("Setting Service with id '{}' as inactive", service.getService().getId());
                infraServiceService.update(service, null);
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with id '{}'", service.getService().getId());
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
                    if (!user.getEmail().equals("") && !user.getEmail().equals(userEmail)) {
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
        if (users.stream().noneMatch(u -> u.getEmail().equals(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
    }

    public void validateScientificDomains(List<ServiceProviderDomain> scientificDomains){
        for (ServiceProviderDomain providerScientificDomain : scientificDomains){
            String[] parts = providerScientificDomain.getScientificSubdomain().split("-");
            String scientificDomain = "scientific_domain-" + parts[1];
            if (!providerScientificDomain.getScientificDomain().equals(scientificDomain)){
                throw new ValidationException("Scientific Subdomain '" + providerScientificDomain.getScientificSubdomain() +
                        "' should have as Scientific Domain the value '" + scientificDomain +"'");
            }
        }
    }

    public void validateMerilScientificDomains(List<ProviderMerilDomain> merilScientificDomains){
        for (ProviderMerilDomain providerMerilScientificDomain : merilScientificDomains){
            String[] parts = providerMerilScientificDomain.getMerilScientificSubdomain().split("-");
            String merilScientificDomain = "provider_meril_scientific_domain-" + parts[1];
            if (!providerMerilScientificDomain.getMerilScientificDomain().equals(merilScientificDomain)){
                throw new ValidationException("Meril Scientific Subdomain '" + providerMerilScientificDomain.getMerilScientificSubdomain() +
                        "' should have as Meril Scientific Domain the value '" + merilScientificDomain +"'");
            }
        }
    }

    // For front-end use
    public boolean validateUrl(URL urlForValidation) {
        try {
            fieldValidator.validateUrl(null, urlForValidation);
        } catch (Throwable e){
            return false;
        }
        return true;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth){
        ProviderBundle providerBundle = get(providerId);
        List<String> userList = new ArrayList<>();
        for (User user : providerBundle.getProvider().getUsers()){
            userList.add(user.getEmail());
        }
        if ((providerBundle.getMetadata().getTerms() == null || providerBundle.getMetadata().getTerms().isEmpty())) {
            if (userList.contains(User.of(auth).getEmail())) {
                return false; //pop-up modal
            } else {
                return true; //no modal
            }
        }
        if (!providerBundle.getMetadata().getTerms().contains(User.of(auth).getEmail()) && userList.contains(User.of(auth).getEmail())) {
            return false; // pop-up modal
        }
        return true; // no modal
    }

    public void adminAcceptedTerms(String providerId, Authentication auth){
        update(get(providerId), auth);
    }

    public void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
        List<String> existingAdmins = new ArrayList<>();
        List<String> newAdmins = new ArrayList<>();
        for (User user : existingProvider.getProvider().getUsers()){
            existingAdmins.add(user.getEmail());
        }
        for (User user : updatedProvider.getProvider().getUsers()){
            newAdmins.add(user.getEmail());
        }
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        if (!adminsAdded.isEmpty()){
            registrationMailService.sendEmailsToNewlyAddedAdmins(updatedProvider, adminsAdded);
        }
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);
        if (!adminsDeleted.isEmpty()){
            registrationMailService.sendEmailsToNewlyDeletedAdmins(existingProvider, adminsDeleted);
        }
    }

    public void requestProviderDeletion(String providerId, Authentication auth) {
        ProviderBundle provider = get(providerId);
        for (User user : provider.getProvider().getUsers()){
            if (user.getEmail().equalsIgnoreCase(User.of(auth).getEmail())){
                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth));
            }
        }
    }

    public String determineRole(Authentication authentication){
        String role;
        if (securityService.hasRole(authentication, "ROLE_ADMIN")) {
            role = "admin";
        } else if (securityService.hasRole(authentication, "ROLE_PROVIDER")){
            role = "provider";
        } else {
            role = "user";
        }
        return role;
    }

    public InfraService updateInfraServiceLoggingInfo(String providerId, String type, Authentication authentication){
        List<Service> providerServices = getServices(providerId);
        List<InfraService> infraServices = new ArrayList<>();
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        for (Service service : providerServices){
            infraServices.add(infraServiceService.get(service.getId()));
        }
        // find the Service Template
        InfraService serviceTemplate = infraServices.get(0);
        for (InfraService infraService : infraServices){
            if (Double.parseDouble(infraService.getMetadata().getRegisteredAt()) < Double.parseDouble(serviceTemplate.getMetadata().getRegisteredAt())){
                serviceTemplate = infraService;
            }
        }
        loggingInfoList = serviceTemplate.getLoggingInfo();
        LoggingInfo loggingInfo = LoggingInfo.updateLoggingInfo(User.of(authentication).getEmail(), determineRole(authentication), type);
        loggingInfoList.add((loggingInfo));
        serviceTemplate.setLoggingInfo(loggingInfoList);
        return serviceTemplate;
    }
}
