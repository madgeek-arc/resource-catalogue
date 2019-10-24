package eu.einfracentral.registry.manager;

import eu.einfracentral.config.security.EICAuthoritiesMapper;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends ResourceManager<Provider> implements ProviderService<Provider, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private SecurityService securityService;
    private Random randomNumberGenerator;
    private RegistrationMailService registrationMailService;
    private EICAuthoritiesMapper eicAuthoritiesMapper;
    private VocabularyService vocabularyService;

    private static final int NAME_LENGTH = 80;
    private static final int FIELD_LENGTH = 100;
    private static final int FIELD_LENGTH_SMALL = 20;
    private static final int TEXT_LENGTH = 1000;

    @Autowired
    public ProviderManager(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                           @Lazy SecurityService securityService, Random randomNumberGenerator,
                           @Lazy RegistrationMailService registrationMailService, /*JmsTemplate jmsTopicTemplate*/
                           @Lazy EICAuthoritiesMapper eicAuthoritiesMapper,
                           VocabularyService vocabularyService) {
        super(Provider.class);
        this.infraServiceService = infraServiceService;
        this.securityService = securityService;
        this.randomNumberGenerator = randomNumberGenerator;
        this.registrationMailService = registrationMailService;
        this.eicAuthoritiesMapper = eicAuthoritiesMapper;
        this.vocabularyService = vocabularyService;
    }


    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public Provider add(Provider provider, Authentication auth) {
        List<User> users;
        User authUser = new User(auth);
        Provider ret;
        validateProvider(provider);
        users = provider.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equals(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
        provider.setActive(false);
        provider.setStatus(Provider.States.PENDING_1.getKey());

        ret = super.add(provider, null);
        logger.debug("Adding Provider: {}", provider);

        // update provider roles
        eicAuthoritiesMapper.updateAuthorities();

        // send messages to queue
        registrationMailService.sendProviderMails(provider);

        return ret;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public Provider update(Provider provider, Authentication auth) {
        validateProvider(provider);
        Resource existing = whereID(provider.getId(), true);
        Provider ex = deserialize(existing);
        provider.setActive(ex.getActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Provider: {}", provider);

        // update provider roles
        eicAuthoritiesMapper.updateAuthorities();

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
    public Provider get(String id) {
        Provider provider = super.get(id);
        if (provider == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find provider with id: %s", id));
        }
        return provider;
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public Provider get(String id, Authentication auth) {
        Provider provider = get(id);
        if (auth == null) {
            provider.setUsers(null);
        } else if (securityService.hasRole(auth, "ROLE_ADMIN")) {
            return provider;
        } else if (securityService.hasRole(auth, "ROLE_PROVIDER")
                && securityService.userIsProviderAdmin(auth, provider)) {
            return provider;
        }
        provider.setUsers(null);
        return provider;
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public Browsing<Provider> getAll(FacetFilter ff, Authentication auth) {
        List<Provider> userProviders = null;
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN")) {
                return super.getAll(ff, auth);
            }
            // if user is not an admin, check if he is a provider
            userProviders = getMyServiceProviders(auth);
        }

        // retrieve providers
        Browsing<Provider> providers = super.getAll(ff, auth);

        // create a list of providers without their users
        List<Provider> modified = providers.getResults()
                .stream()
                .map(p -> {
                    p.setUsers(null);
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
    public void delete(Provider provider) {
        List<InfraService> services = this.getInfraServices(provider.getId());
        services.forEach(s -> {
            try {
                if (s.getProviders().size() == 1) {
                    infraServiceService.delete(s);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Error deleting Service", e);
            }
        });
        super.delete(provider);
        logger.debug("Deleting Provider: {}", provider);

        // update provider roles
        eicAuthoritiesMapper.updateAuthorities();
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public Provider verifyProvider(String id, Provider.States status, Boolean active, Authentication auth) {
        Provider provider = get(id);
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
    public List<Provider> getServiceProviders(String email, Authentication auth) {
        List<Provider> providers;
        if (auth == null) {
//            return null; // TODO: enable this when front end can handle 401 properly
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
                    if (p.getUsers() != null && p.getUsers().stream().filter(Objects::nonNull).anyMatch(u -> {
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
    public List<Provider> getMyServiceProviders(Authentication auth) {
        if (auth == null) {
//            return null; // TODO: enable this when front end can handle 401 properly
            return new ArrayList<>();
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return super.getAll(ff, null).getResults()
                .stream().map(p -> {
                    if (securityService.userIsProviderAdmin(auth, p)) {
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
        return infraServiceService.getAll(ff, null).getResults();
    }

    @Override
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("latest", "true");
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults().stream().map(Service::new).collect(Collectors.toList());
    }

    @Override
    public List<Service> getActiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults().stream().map(Service::new).collect(Collectors.toList());
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
    public List<Provider> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        return getAll(ff, null).getResults();
    }

    @Override
    public List<InfraService> getInactiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
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
                logger.info("Setting Service with name '{}' as active", service.getName());
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}", service.getName());
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
                logger.info("Setting Service with name '{}' as inactive", service.getName());
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update service with name '{}'", service.getName());
            }
        }
    }

    /**
     * This method is used to update a list of new providers with status 'Provider.States.ST_SUBMISSION'
     * to status 'Provider.States.PENDING_2'
     * @param providers
     * @param authentication
     */
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void verifyNewProviders(List<String> providers, Authentication authentication) {
        for (String serviceProvider : providers) {
            Provider provider = get(serviceProvider);
            if (provider.getStatus().equals(Provider.States.ST_SUBMISSION.getKey())) {
                verifyProvider(provider.getId(), Provider.States.PENDING_2, false, authentication);
            }
        }
    }

    public void validateProvider(Provider provider){
        logger.debug("Validating vocabularies, Provider id: {}", provider.getId());
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();

        // Validate Provider's ID
        if (provider.getId() == null) {
            provider.setId(provider.getName());
        }
        provider.setId(StringUtils
                .stripAccents(provider.getId())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
        if ("".equals(provider.getId())) {
            throw new ServiceException("Provider id not valid. Special characters are ignored.");
        }

        // Validate Provider's Name
        if (provider.getName() == null || provider.getName().equals("")) {
            throw new ValidationException("field 'name' is obligatory");
        }
        if (provider.getName().length() > NAME_LENGTH) {
            throw new ValidationException("max length for 'name' is 80 chars");
        }

        // Validate Provider's Acronym
        if (provider.getAcronym() == null || provider.getAcronym().equals("")) {
            throw new ValidationException("field 'acronym' is obligatory");
        }
        if (provider.getAcronym().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'name' is 20 chars");
        }

        // Validate Provider's Website
        if (provider.getWebsite() == null || provider.getWebsite().toString().equals("")) {
            throw new ValidationException("field 'website' is obligatory");
        }

        // Validate Provider's Description
        if (provider.getDescription() == null || provider.getDescription().equals("")) {
            throw new ValidationException("field 'description' is obligatory");
        }
        if (provider.getDescription().length() > TEXT_LENGTH) {
            throw new ValidationException("max length for 'description' is 1000 chars");
        }

        // Validate Provider's Logo
        if (provider.getLogo() == null || provider.getLogo().toString().equals("")) {
            throw new ValidationException("field 'logo' is obligatory");
        }

        // Validate Provider's Type
        if (provider.getTypes() == null || provider.getTypes().isEmpty())
            throw new ValidationException("Field 'types' is mandatory.");
        for (String type : provider.getTypes()) {
            if (!allVocabularies.containsKey(type))
                throw new ValidationException(String.format("type '%s' does not exist.", type));
        }

        // Validate Provider's Category
        if (provider.getCategories() == null || provider.getCategories().isEmpty())
            throw new ValidationException("Field 'categories' is mandatory.");
        for (String category : provider.getCategories()) {
            if (!allVocabularies.containsKey(category))
                throw new ValidationException(String.format("category '%s' does not exist.", category));
        }

        // Validate Provider's ESFRI Domain
        if (provider.getEsfriDomains() == null || provider.getEsfriDomains().isEmpty())
            throw new ValidationException("Field 'esfriDomains' is mandatory.");
        for (String esfriDomain : provider.getEsfriDomains()) {
            if (!allVocabularies.containsKey(esfriDomain))
                throw new ValidationException(String.format("esfriDomain '%s' does not exist.", esfriDomain));
        }

        // Validate Provider's Tags
        if (provider.getTags() != null) {
            if (provider.getTags().size() == 1 && "".equals(provider.getTags().get(0))) {
                provider.getTags().remove(0);
            }
            for (String tag : provider.getTags()) {
                if (tag != null && tag.length() > FIELD_LENGTH_SMALL) {
                    throw new ValidationException("max length for 'tag' is " + FIELD_LENGTH_SMALL + " chars");
                }
                if (tag == null || tag.equals("")) {
                    throw new ValidationException("One or more items of the tags list is null or empty");
                }
            }
        }

        // Validate Provider's Life Cycle Status
        if (provider.getLifeCycleStatus() == null || provider.getLifeCycleStatus().isEmpty())
            throw new ValidationException("Field 'lifeCycleStatus' is mandatory.");
        if (!allVocabularies.containsKey(provider.getLifeCycleStatus()))
            throw new ValidationException(String.format("lifeCycleStatus '%s' does not exist.", provider.getLifeCycleStatus()));

        // Validate Provider's Location
        if (provider.getLocation() == null){
            throw new ValidationException("Field 'location' is mandatory.");
        }
        if (provider.getLocation().getName() == null || provider.getLocation().getName().equals("")) {
            throw new ValidationException("field 'name' is mandatory");
        }
        if (provider.getLocation().getName().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
        }
        if (provider.getLocation().getStreet() == null || provider.getLocation().getStreet().equals("")) {
            throw new ValidationException("field 'street' is mandatory");
        }
        if (provider.getLocation().getStreet().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
        }
        if (provider.getLocation().getNumber() == null || provider.getLocation().getNumber().equals("")) {
            throw new ValidationException("field 'number' is mandatory");
        }
        if (provider.getLocation().getNumber().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
        }
        if (provider.getLocation().getPostalCode() == null || provider.getLocation().getPostalCode().equals("")) {
            throw new ValidationException("field 'postalCode' is mandatory");
        }
        if (provider.getLocation().getPostalCode().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
        }
        if (provider.getLocation().getCity() == null || provider.getLocation().getCity().equals("")) {
            throw new ValidationException("field 'city' is mandatory");
        }
        if (provider.getLocation().getCity().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
        }
        if (provider.getLocation().getRegion() == null || provider.getLocation().getRegion().equals("")) {
            throw new ValidationException("field 'region' is mandatory");
        }
        if (provider.getLocation().getRegion().length() > FIELD_LENGTH_SMALL) {
            throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
        }

        // Validate Coordinating Country
        if (provider.getCoordinatingCountry() != null && !provider.getCoordinatingCountry().equals("")) {
            if (!allVocabularies.containsKey(provider.getCoordinatingCountry()))
                throw new ValidationException(String.format("coordinatingCountry '%s' does not exist.", provider.getCoordinatingCountry()));
        } else throw new ValidationException("Field 'coordinatingCountry' is mandatory.");

        // Validate Participating Countries
        if (provider.getParticipatingCountries() != null && !provider.getParticipatingCountries().isEmpty()) {
            List<String> notFoundCountries = new ArrayList<>();
            List<String> foundCountries = new ArrayList<>();
            for (String countryId : provider.getParticipatingCountries()) {
                if (allVocabularies.containsKey(countryId)) {
                    if (!foundCountries.contains(countryId)) {
                        foundCountries.add(countryId);
                    }
                } else {
                    notFoundCountries.add(countryId);
                }
            }
            if (!notFoundCountries.isEmpty()) {
                throw new ValidationException(String.format("Countries not found: %s",
                        String.join(", ", notFoundCountries)));
            }
            provider.setParticipatingCountries(foundCountries);
        }

        // Validate Provider's Contacts
        if (provider.getContacts() == null || provider.getContacts().isEmpty())
            throw new ValidationException("Field 'contacts' is mandatory. You need to provide at least 1 contact.");
        for (Contact contact : provider.getContacts()) {

        // Validate the Contact's fields requirement
            if (contact.getFirstName() == null || contact.getFirstName().equals("")) {
                throw new ValidationException("field 'firstName' is mandatory");
            }
            if (contact.getLastName() == null || contact.getLastName().equals("")) {
                throw new ValidationException("field 'lastName' is mandatory");
            }
            if (contact.getEmail() == null || contact.getEmail().equals("")) {
                throw new ValidationException("field 'email' is mandatory");
            }
            if (contact.getTel() == null || contact.getTel().equals("")) {
                 throw new ValidationException("field 'tel' is mandatory");
            }

            // Validate max length of Contact's fields
            if (contact.getFirstName().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
            }
            if (contact.getLastName().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'lastName' is " + FIELD_LENGTH_SMALL + " chars");
            }
            if (contact.getTel().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'tel' is " + FIELD_LENGTH_SMALL + " chars");
            }
            if (contact.getPosition().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'position' is " + FIELD_LENGTH_SMALL + " chars");
            }
        }

        // Validate Provider's Hosting Legal Entity
        if (provider.getHostingLegalEntity() != null && provider.getHostingLegalEntity().length() > NAME_LENGTH) {
            throw new ValidationException("max length for 'hostingLegalEntity' is " + NAME_LENGTH + " chars");
        }

        // Validate Provider's Legal Status
        if (provider.getLegalStatus() != null && !allVocabularies.containsKey(provider.getLegalStatus())){
            throw new ValidationException(String.format("legalStatus '%s' does not exist.", provider.getLegalStatus()));
        }

        // Validate Provider's ESFRI
        if (provider.getEsfri() != null && !allVocabularies.containsKey(provider.getEsfri())){
            throw new ValidationException(String.format("ESFRI '%s' does not exist.", provider.getEsfri()));
        }

        // Validate Provider's Networks
        if (provider.getNetworks() != null) {
            for (String network : provider.getNetworks()) {
                if (!allVocabularies.containsKey(network))
                    throw new ValidationException(String.format("network '%s' does not exist.", network));
            }
        }

        // Validate Provider's Areas of Activity
        if (provider.getAreasOfActivity() != null) {
            for (String area : provider.getAreasOfActivity()) {
                if (!allVocabularies.containsKey(area))
                    throw new ValidationException(String.format("areaOfActivity '%s' does not exist.", area));
            }
        }

        // Validate Provider's Societal Grand Challenges
        if (provider.getSocietalGrandChallenges() != null) {
            for (String challenge : provider.getSocietalGrandChallenges()) {
                if (!allVocabularies.containsKey(challenge))
                    throw new ValidationException(String.format("societalGrandChallenge '%s' does not exist.", challenge));
            }
        }

    }

}
