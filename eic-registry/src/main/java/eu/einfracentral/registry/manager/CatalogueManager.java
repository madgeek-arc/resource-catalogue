package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.einfracentral.config.CacheConfig.CACHE_CATALOGUES;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;
import static org.junit.Assert.assertTrue;

@org.springframework.stereotype.Service("catalogueManager")
public class CatalogueManager extends ResourceManager<CatalogueBundle> implements CatalogueService<CatalogueBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(CatalogueManager.class);
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final IdCreator idCreator;

    @Autowired
    public CatalogueManager(@Lazy SecurityService securityService, @Lazy VocabularyService vocabularyService,
                            IdCreator idCreator) {
        super(CatalogueBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceType() {
        return "catalogue";
    }

    @Override
    @Cacheable(value = CACHE_CATALOGUES)
    public CatalogueBundle get(String id) {
        CatalogueBundle catalogue = super.get(id);
        if (catalogue == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", id));
        }
        return catalogue;
    }

    @Override
    @Cacheable(value = CACHE_CATALOGUES)
    public CatalogueBundle get(String id, Authentication auth) {
        CatalogueBundle catalogueBundle = get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Catalogue Admin on the specific Catalogue, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsCatalogueAdmin(user, id)) {
                return catalogueBundle;
            }
        }
        //TODO: ACTIVATE THIS IF CATALOGUE GET A STATUS FIELD
//        // else return the Provider ONLY if he is active
//        if (catalogueBundle.getStatus().equals(vocabularyService.get("approved catalogue").getId())){
//            return catalogueBundle;
//        }
        throw new ValidationException("You cannot view the specific Catalogue");
    }

    @Override
    @CacheEvict(value = CACHE_CATALOGUES, allEntries = true)
    public CatalogueBundle add(CatalogueBundle catalogue, Authentication auth) {

        catalogue.setId(idCreator.createCatalogueId(catalogue.getCatalogue()));
        logger.trace("User '{}' is attempting to add a new Provider: {}", auth, catalogue);
        addAuthenticatedUser(catalogue.getCatalogue(), auth);
        validate(catalogue);
        if (catalogue.getCatalogue().getScientificDomains() != null && !catalogue.getCatalogue().getScientificDomains().isEmpty()) {
            validateScientificDomains(catalogue.getCatalogue().getScientificDomains());
        }
        validateEmailsAndPhoneNumbers(catalogue);
        catalogue.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add((loggingInfo));
        catalogue.setLoggingInfo(loggingInfoList);
        catalogue.setActive(false);
        //TODO: ACTIVATE THIS IF CATALOGUE GET A STATUS FIELD
//        catalogue.setStatus(vocabularyService.get("pending catalogue").getId());

        // latestOnboardingInfo
        catalogue.setLatestOnboardingInfo(loggingInfo);

        if (catalogue.getCatalogue().getParticipatingCountries() != null && !catalogue.getCatalogue().getParticipatingCountries().isEmpty()){
            catalogue.getCatalogue().setParticipatingCountries(sortCountries(catalogue.getCatalogue().getParticipatingCountries()));
        }

        CatalogueBundle ret;
        ret = super.add(catalogue, null);
        logger.debug("Adding Provider: {}", catalogue);

//        registrationMailService.sendEmailsToNewlyAddedAdmins(catalogue, null);
//
//        jmsTopicTemplate.convertAndSend("catalogue.create", catalogue);
//
//        synchronizerServiceProvider.syncAdd(catalogue.getCatalogue());

        return ret;
    }

    @CacheEvict(value = CACHE_CATALOGUES, allEntries = true)
    public CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Catalogue with id '{}'", auth, catalogue);
        validate(catalogue);
        if (catalogue.getCatalogue().getScientificDomains() != null && !catalogue.getCatalogue().getScientificDomains().isEmpty()) {
            validateScientificDomains(catalogue.getCatalogue().getScientificDomains());
        }
        validateEmailsAndPhoneNumbers(catalogue);
        catalogue.setMetadata(Metadata.updateMetadata(catalogue.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey(), comment);
        if (catalogue.getLoggingInfo() != null) {
            loggingInfoList = catalogue.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        catalogue.getCatalogue().setParticipatingCountries(sortCountries(catalogue.getCatalogue().getParticipatingCountries()));
        catalogue.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        catalogue.setLatestUpdateInfo(loggingInfo);

        Resource existing = whereID(catalogue.getId(), true);
        CatalogueBundle ex = deserialize(existing);
        catalogue.setActive(ex.isActive());
        //TODO: ACTIVATE THIS IF CATALOGUE GET A STATUS FIELD
//        catalogue.setStatus(ex.getStatus());
        existing.setPayload(serialize(catalogue));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Catalogue: {}", catalogue);

        // Send emails to newly added or deleted Admins
        adminDifferences(catalogue, ex);

        //TODO: ACTIVATE THIS WHEN WE CREATE CATALOGUE EMAILS
        // send notification emails to Portal Admins
//        if (catalogue.getLatestAuditInfo() != null && catalogue.getLatestUpdateInfo() != null) {
//            Long latestAudit = Long.parseLong(catalogue.getLatestAuditInfo().getDate());
//            Long latestUpdate = Long.parseLong(catalogue.getLatestUpdateInfo().getDate());
//            if (latestAudit < latestUpdate && catalogue.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(catalogue);
//            }
//        }

//        jmsTopicTemplate.convertAndSend("catalogue.update", catalogue);
//
//        synchronizerServiceProvider.syncUpdate(catalogue.getCatalogue());

        return catalogue;
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public Browsing<CatalogueBundle> getAll(FacetFilter ff, Authentication auth) {
        //TODO: REFACTOR IF WE ENABLE STATUS FIELD IN CATALOGUE
        return super.getAll(ff, auth);
    }

    private void addAuthenticatedUser(Catalogue catalogue, Authentication auth) {
        List<User> users;
        User authUser = User.of(auth);
        users = catalogue.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(authUser.getEmail()))) {
            users.add(authUser);
            catalogue.setUsers(users);
        }
    }

    public void validateScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        for (ServiceProviderDomain catalogueScientificDomain : scientificDomains) {
            String[] parts = catalogueScientificDomain.getScientificSubdomain().split("-");
            String scientificDomain = "scientific_domain-" + parts[1];
            if (!catalogueScientificDomain.getScientificDomain().equals(scientificDomain)) {
                throw new ValidationException("Scientific Subdomain '" + catalogueScientificDomain.getScientificSubdomain() +
                        "' should have as Scientific Domain the value '" + scientificDomain + "'");
            }
        }
    }

    public void validateEmailsAndPhoneNumbers(CatalogueBundle catalogueBundle){
        EmailValidator validator = EmailValidator.getInstance();
        Pattern pattern = Pattern.compile("^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$");
        // main contact email
        String mainContactEmail = catalogueBundle.getCatalogue().getMainContact().getEmail();
        if (!validator.isValid(mainContactEmail)) {
            throw new ValidationException(String.format("Email [%s] is not valid. Found in field Main Contact Email", mainContactEmail));
        }
        // main contact phone
        if (catalogueBundle.getCatalogue().getMainContact().getPhone() != null && !catalogueBundle.getCatalogue().getMainContact().getPhone().equals("")){
            String mainContactPhone = catalogueBundle.getCatalogue().getMainContact().getPhone();
            Matcher mainContactPhoneMatcher = pattern.matcher(mainContactPhone);
            try {
                assertTrue(mainContactPhoneMatcher.matches());
            } catch(AssertionError e){
                throw new ValidationException(String.format("The phone you provided [%s] is not valid. Found in field Main Contact Phone", mainContactPhone));
            }
        }
        // public contact
        for (ProviderPublicContact providerPublicContact : catalogueBundle.getCatalogue().getPublicContacts()){
            // public contact email
            if (providerPublicContact.getEmail() != null && !providerPublicContact.getEmail().equals("")){
                String publicContactEmail = providerPublicContact.getEmail();
                if (!validator.isValid(publicContactEmail)) {
                    throw new ValidationException(String.format("Email [%s] is not valid. Found in field Public Contact Email", publicContactEmail));
                }
            }
            // public contact phone
            if (providerPublicContact.getPhone() != null && !providerPublicContact.getPhone().equals("")){
                String publicContactPhone = providerPublicContact.getPhone();
                Matcher publicContactPhoneMatcher = pattern.matcher(publicContactPhone);
                try {
                    assertTrue(publicContactPhoneMatcher.matches());
                } catch(AssertionError e){
                    throw new ValidationException(String.format("The phone you provided [%s] is not valid. Found in field Public Contact Phone", publicContactPhone));
                }
            }
        }
        // user email
        for (User user : catalogueBundle.getCatalogue().getUsers()){
            if (user.getEmail() != null && !user.getEmail().equals("")){
                String userEmail = user.getEmail();
                if (!validator.isValid(userEmail)) {
                    throw new ValidationException(String.format("Email [%s] is not valid. Found in field User Email", userEmail));
                }
            }
        }
    }

    public List<String> sortCountries(List<String> countries) {
        Collections.sort(countries);
        return countries;
    }

    public void adminDifferences(CatalogueBundle updatedCatalogue, CatalogueBundle existingCatalogue) {
        List<String> existingAdmins = new ArrayList<>();
        List<String> newAdmins = new ArrayList<>();
        for (User user : existingCatalogue.getCatalogue().getUsers()) {
            existingAdmins.add(user.getEmail().toLowerCase());
        }
        for (User user : updatedCatalogue.getCatalogue().getUsers()) {
            newAdmins.add(user.getEmail().toLowerCase());
        }
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        //TODO: ACTIVATE THIS WHEN WE CREATE CATALOGUE EMAILS
//        if (!adminsAdded.isEmpty()) {
//            registrationMailService.sendEmailsToNewlyAddedAdmins(updatedProvider, adminsAdded);
//        }
//        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
//        adminsDeleted.removeAll(newAdmins);
//        if (!adminsDeleted.isEmpty()) {
//            registrationMailService.sendEmailsToNewlyDeletedAdmins(existingProvider, adminsDeleted);
//        }
    }
}
