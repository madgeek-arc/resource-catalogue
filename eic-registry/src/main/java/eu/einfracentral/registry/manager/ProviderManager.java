package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
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
import org.springframework.security.core.Authentication;

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
    private final RegistrationMailService registrationMailService;

    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private EventService eventService;

    @Autowired
    public ProviderManager(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                           @Lazy SecurityService securityService, Random randomNumberGenerator,
                           @Lazy RegistrationMailService registrationMailService,
                           @Lazy FieldValidator fieldValidator,
                           IdCreator idCreator, EventService eventService) {
        super(ProviderBundle.class);
        this.infraServiceService = infraServiceService;
        this.securityService = securityService;
        this.randomNumberGenerator = randomNumberGenerator;
        this.registrationMailService = registrationMailService;
        this.fieldValidator = fieldValidator;
        this.idCreator = idCreator;
        this.eventService = eventService;
    }


    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle provider, Authentication auth) {

        provider.setId(idCreator.createProviderId(provider.getProvider()));
        addAuthenticatedUser(provider.getProvider(), auth);
        validate(provider);

        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        provider.setActive(false);
        provider.setStatus(Provider.States.PENDING_1.getKey());

        ProviderBundle ret;
        ret = super.add(provider, null);
        logger.debug("Adding Provider: {}", provider);

        // send messages to queue
        registrationMailService.sendProviderMails(provider);

        return ret;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle provider, Authentication auth) {
        validate(provider);
        provider.setMetadata(Metadata.updateMetadata(provider.getMetadata(), User.of(auth).getFullName()));
        Resource existing = whereID(provider.getId(), true);
        ProviderBundle ex = deserialize(existing);
        provider.setActive(ex.isActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Provider: {}", provider);

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
        ProviderBundle provider = get(id);
        if (auth == null) {
            provider.getProvider().setUsers(null);
        } else if (securityService.hasRole(auth, "ROLE_ADMIN")) {
            return provider;
        } else if (securityService.hasRole(auth, "ROLE_PROVIDER")
                && securityService.userIsProviderAdmin(auth, provider.getId())) {
            return provider;
        }
        provider.getProvider().setUsers(null);
        return provider;
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

        // create a list of providers without their users
        List<ProviderBundle> modified = providers.getResults()
                .stream()
                .map(p -> {
                    p.getProvider().setUsers(null);
                    return p;
                })
                .collect(Collectors.toList());

        if (userProviders != null) {
            // replace user providers having null users with complete provider entries
            userProviders.forEach(x -> {
                modified.removeIf(provider -> provider.getId().equals(x.getId()));
                modified.add(x);
            });
        }
        providers.setResults(modified);
        return providers;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(ProviderBundle provider) {
        List<InfraService> services = this.getInfraServices(provider.getId());
        services.forEach(s -> {
            try {
                if (s.getService().getProviders().size() == 1) {
                    infraServiceService.delete(s);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Error deleting Service", e);
            }
        });
        super.delete(provider);
        logger.debug("Deleting Provider: {}", provider);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle verifyProvider(String id, Provider.States status, Boolean active, Authentication auth) {
        ProviderBundle provider = get(id);
        provider.setStatus(status.getKey());
        switch (status) {
            case APPROVED:
                if (active == null) {
                    active = true;
                }
                provider.setActive(active);
                break;

            default:
                provider.setActive(false);
        }

        // send registration emails
        registrationMailService.sendProviderMails(provider);

        if (active != null) {
            provider.setActive(active);
            if (!active) {
                deactivateServices(provider.getId());
            } else {
                activateServices(provider.getId());
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
            return new ArrayList<>();
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
            return new ArrayList<>();
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return super.getAll(ff, auth).getResults()
                .stream().map(p -> {
                    if (securityService.userIsProviderAdmin(auth, p.getId())) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<InfraService> getInfraServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults();
    }

    @Override
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("latest", "true");
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    @Override
    public List<Service> getActiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
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
        ff.addFilter("providers", providerId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return infraServiceService.getAll(ff, null).getResults();
    }

    public void activateServices(String providerId) { // TODO: decide how to use service.status variable
        List<InfraService> services = this.getInfraServices(providerId);
        logger.info("Activating all Services of the Provider with id: {}", providerId);
        for (InfraService service : services) {
//            service.setActive(service.getStatus() == null || service.getStatus().equals("true"));
//            service.setStatus(null);
            service.setActive(true);
            try {
                infraServiceService.update(service, null);
                logger.info("Setting Service with name '{}' as active", service.getService().getName());
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}", service.getService().getName());
            }
        }
    }

    public void deactivateServices(String providerId) { // TODO: decide how to use service.status variable
        List<InfraService> services = this.getInfraServices(providerId);
        for (InfraService service : services) {
//            service.setStatus(service.isActive() != null ? service.isActive().toString() : "true");
//            service.setStatus(null);
            service.setActive(false);
            try {
                infraServiceService.update(service, null);
                logger.info("Setting Service with name '{}' as inactive", service.getService().getName());
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}'", service.getService().getName());
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

        // Validate Provider's National Roadmap
        if (provider.getProvider().getNationalRoadmap() != null) {
            if (!"yes".equalsIgnoreCase(provider.getProvider().getNationalRoadmap())
                    && !"no".equalsIgnoreCase(provider.getProvider().getNationalRoadmap())) {
                throw new ValidationException("nationalRoadmap's value should be Yes or No");
            }
        }
        return provider;
    }

    @Override
    @CacheEvict(value = {CACHE_PROVIDERS, CACHE_SERVICE_EVENTS, CACHE_EVENTS}, allEntries = true)
    public void deleteUserInfo(Authentication authentication) {
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

}
