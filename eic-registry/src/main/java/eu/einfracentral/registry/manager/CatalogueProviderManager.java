package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.*;

@Service("catalogueProviderManager")
public class CatalogueProviderManager extends ResourceManager<ProviderBundle> implements CatalogueProviderService<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(CatalogueManager.class);
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final IdCreator idCreator;
    private final JmsTemplate jmsTopicTemplate;
    private final FieldValidator fieldValidator;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final RegistrationMailService registrationMailService;
    private final javax.sql.DataSource dataSource;

    @Autowired
    public CatalogueProviderManager(@Lazy SecurityService securityService, @Lazy VocabularyService vocabularyService,
                            IdCreator idCreator, JmsTemplate jmsTopicTemplate, ProviderService<ProviderBundle, Authentication> providerService,
                            FieldValidator fieldValidator, DataSource dataSource, CatalogueService<CatalogueBundle, Authentication> catalogueService,
                            @Lazy RegistrationMailService registrationMailService) {
        super(ProviderBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.idCreator = idCreator;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.fieldValidator = fieldValidator;
        this.providerService = providerService;
        this.catalogueService = catalogueService;
        this.dataSource = dataSource;
        this.registrationMailService = registrationMailService;
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Cacheable(value = CACHE_PROVIDERS)
    public ProviderBundle getCatalogueProvider(String catalogueId, String providerId, Authentication auth) {
        ProviderBundle providerBundle = getWithCatalogueId(providerId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (providerBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find provider with id: %s", providerId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", catalogueId));
        }
        if (!providerBundle.getProvider().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Provider with id [%s] does not belong to the catalogue with id [%s]", providerId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            //TODO: userIsCatalogueAdmin -> transcationRollback error
            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
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
    @Cacheable(value = CACHE_PROVIDERS)
    public Browsing<ProviderBundle> getAllCatalogueProviders(FacetFilter ff, Authentication auth) {
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
            userProviders = providerService.getMyServiceProviders(auth);
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
    public ProviderBundle addCatalogueProvider(ProviderBundle provider, String catalogueId, Authentication auth) {
        checkCatalogueIdConsistency(provider, catalogueId);
        provider.setId(idCreator.createProviderId(provider.getProvider()));
        logger.trace("User '{}' is attempting to add a new Provider: {} on Catalogue: {}", auth, provider, catalogueId);
        addAuthenticatedUser(provider.getProvider(), auth);
        providerService.validate(provider);

        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        provider.setLoggingInfo(loggingInfoList);
        provider.setActive(true);
        provider.setStatus(vocabularyService.get("approved provider").getId());
        provider.setTemplateStatus(vocabularyService.get("approved template").getId());

        // latestOnboardingInfo
        provider.setLatestOnboardingInfo(loggingInfo);

        if (provider.getProvider().getParticipatingCountries() != null && !provider.getProvider().getParticipatingCountries().isEmpty()){
            provider.getProvider().setParticipatingCountries(sortCountries(provider.getProvider().getParticipatingCountries()));
        }

        ProviderBundle ret;
        ret = add(provider, null);
        logger.debug("Adding Provider: {} of Catalogue: {}", provider, catalogueId);

        registrationMailService.sendEmailsToNewlyAddedAdmins(provider, null);

//        jmsTopicTemplate.convertAndSend("provider.create", provider);
//        synchronizerServiceProvider.syncAdd(provider.getProvider());

        return ret;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle updateCatalogueProvider(ProviderBundle provider, String catalogueId, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Provider with id '{}' of the Catalogue '{}'", auth, provider, provider.getProvider().getCatalogueId());
        checkCatalogueIdConsistency(provider, catalogueId);
        providerService.validate(provider);

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
        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")){
            if (!ex.getProvider().getCatalogueId().equals(provider.getProvider().getCatalogueId())){
                throw new ValidationException("You cannot change catalogueId");
            }
        }
        provider.setActive(ex.isActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Provider: {} of Catalogue {}", provider, provider.getProvider().getCatalogueId());

        // Send emails to newly added or deleted Admins
        providerService.adminDifferences(provider, ex);

        if (provider.getLatestAuditInfo() != null && provider.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(provider.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(provider.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && provider.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(provider);
            }
        }

//        jmsTopicTemplate.convertAndSend("provider.update", provider);
//        synchronizerServiceProvider.syncUpdate(provider.getProvider());

        return provider;
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

    public List<String> sortCountries(List<String> countries) {
        Collections.sort(countries);
        return countries;
    }

    public void checkCatalogueIdConsistency(ProviderBundle provider, String catalogueId){
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", catalogueId));
        }
        if (provider.getProvider().getCatalogueId() == null || provider.getProvider().getCatalogueId().equals("")){
            throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
        } else{
            if (!provider.getProvider().getCatalogueId().equals(catalogueId)){
                throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
            }
        }
    }


    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ProviderBundle add(ProviderBundle providerBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Provider: {}", auth, providerBundle);
        if (providerBundle.getProvider().getId() == null) {
            providerBundle.getProvider().setId(idCreator.createProviderId(providerBundle.getProvider()));
        }

        if (exists(providerBundle)) {
            throw new ResourceException(String.format("Provider with id: %s already exists in the Catalogue with id: %s",
                    providerBundle.getProvider().getId(), providerBundle.getProvider().getCatalogueId()), HttpStatus.CONFLICT);
        }

        String serialized;
        serialized = parserPool.serialize(providerBundle, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);

        return providerBundle;
    }

    public ProviderBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find provider with id: %s and catalogueId %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    public ProviderBundle getWithCatalogueId(String id, String catalogueId) {
        return get(id, catalogueId);
    }


    public Resource getResource(String providerId, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("provider_id = \"%s\" AND catalogue_id = \"%s\"", providerId, catalogueId), resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    public boolean exists(ProviderBundle providerBundle) {
        return getResource(providerBundle.getProvider().getId(), providerBundle.getProvider().getCatalogueId()) != null;
    }

}
