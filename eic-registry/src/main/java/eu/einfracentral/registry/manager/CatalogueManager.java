package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Service;


import javax.sql.DataSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_CATALOGUES;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;
import static org.junit.Assert.assertTrue;

@Service("catalogueManager")
public class CatalogueManager extends ResourceManager<CatalogueBundle> implements CatalogueService<CatalogueBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(CatalogueManager.class);
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final IdCreator idCreator;
    private final JmsTemplate jmsTopicTemplate;
    private final FieldValidator fieldValidator;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final ResourceManager<ProviderBundle> providerResourceManager;
    private final DataSource dataSource;
    private final String columnsOfInterest = "catalogue_id, name, abbreviation, affiliations, tags, networks, scientific_subdomains"; // variable with DB tables a keyword is been searched on

    @Autowired
    public CatalogueManager(@Lazy SecurityService securityService, @Lazy VocabularyService vocabularyService,
                            IdCreator idCreator, JmsTemplate jmsTopicTemplate, ProviderService<ProviderBundle, Authentication> providerService,
                            FieldValidator fieldValidator, InfraServiceService<InfraService, InfraService> infraServiceService,
                            @Qualifier("providerManager") ResourceManager<ProviderBundle> providerResourceManager, DataSource dataSource) {
        super(CatalogueBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.idCreator = idCreator;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.fieldValidator = fieldValidator;
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
        this.providerResourceManager = providerResourceManager;
        this.dataSource = dataSource;
    }

    @Override
    public String getResourceType() {
        return "catalogue";
    }

    //SECTION: CATALOGUE
    @Override
    @Cacheable(value = CACHE_CATALOGUES)
    public CatalogueBundle get(String id) {
        CatalogueBundle catalogue = super.get(id);
        if (catalogue == null) {
            throw new ResourceNotFoundException(
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
    @Cacheable(value = CACHE_CATALOGUES)
    public Browsing<CatalogueBundle> getAll(FacetFilter ff, Authentication auth) {
        List<CatalogueBundle> userCatalogues = null;
        List<CatalogueBundle> retList = new ArrayList<>();

        // if user is ADMIN or EPOT return everything
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                    securityService.hasRole(auth, "ROLE_EPOT")) {
                return super.getAll(ff, auth);
            }
            // if user is CATALOGUE ADMIN return all his Catalogues (rejected, pending) with their sensitive data (Users, MainContact) too
            User user = User.of(auth);
            Browsing<CatalogueBundle> catalogues = super.getAll(ff, auth);
            for (CatalogueBundle catalogueBundle : catalogues.getResults()){
                if (catalogueBundle.getStatus().equals(vocabularyService.get("approved catalogue").getId()) ||
                        securityService.userIsCatalogueAdmin(user, catalogueBundle.getId())) {
                    retList.add(catalogueBundle);
                }
            }
            catalogues.setResults(retList);
            catalogues.setTotal(retList.size());
            catalogues.setTo(retList.size());
            userCatalogues = getMyCatalogues(auth);
            if (userCatalogues != null) {
                // replace user providers having null users with complete provider entries
                userCatalogues.forEach(x -> {
                    catalogues.getResults().removeIf(catalogue -> catalogue.getId().equals(x.getId()));
                    catalogues.getResults().add(x);
                });
            }
            return catalogues;
        }

        // else return ONLY approved Catalogues
        ff.addFilter("status", "approved catalogue");
        Browsing<CatalogueBundle> catalogues = super.getAll(ff, auth);
        retList.addAll(catalogues.getResults());
        catalogues.setResults(retList);

        return catalogues;
    }

    @Override
    @CacheEvict(value = CACHE_CATALOGUES, allEntries = true)
    public CatalogueBundle add(CatalogueBundle catalogue, Authentication auth) {

        if (catalogue.getId() == null || catalogue.getId().equals("")){
            catalogue.setId(idCreator.createCatalogueId(catalogue.getCatalogue()));
        }
        logger.trace("User '{}' is attempting to add a new Catalogue: {}", auth, catalogue);
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
        catalogue.setStatus(vocabularyService.get("pending catalogue").getId());

        // latestOnboardingInfo
        catalogue.setLatestOnboardingInfo(loggingInfo);

        if (catalogue.getCatalogue().getParticipatingCountries() != null && !catalogue.getCatalogue().getParticipatingCountries().isEmpty()){
            catalogue.getCatalogue().setParticipatingCountries(sortCountries(catalogue.getCatalogue().getParticipatingCountries()));
        }

        CatalogueBundle ret;
        ret = super.add(catalogue, null);
        logger.debug("Adding Catalogue: {}", catalogue);

//        registrationMailService.sendEmailsToNewlyAddedAdmins(catalogue, null);
//
        jmsTopicTemplate.convertAndSend("catalogue.create", catalogue);
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
        catalogue.setStatus(ex.getStatus());
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
    public CatalogueBundle validate(CatalogueBundle catalogue) {
        logger.debug("Validating Catalogue with id: {}", catalogue.getId());

        try {
            fieldValidator.validateFields(catalogue.getCatalogue());
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        return catalogue;
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS)
    public List<CatalogueBundle> getMyCatalogues(Authentication auth) {
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        User user = User.of(auth);
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return super.getAll(ff, auth).getResults()
                .stream().map(p -> {
                    if (securityService.userIsCatalogueAdmin(user, p.getId())) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogueBundle> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return getAll(ff, null).getResults();
    }

    private void addAuthenticatedUser(Object object, Authentication auth) {
        if (object instanceof Catalogue){
            Catalogue catalogue = (Catalogue) object;
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
        } else{
            Provider provider = (Provider) object;
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

    public boolean hasAdminAcceptedTerms(String catalogueId, Authentication auth) {
        CatalogueBundle catalogueBundle = get(catalogueId);
        List<String> userList = new ArrayList<>();
        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            userList.add(user.getEmail().toLowerCase());
        }
        if ((catalogueBundle.getMetadata().getTerms() == null || catalogueBundle.getMetadata().getTerms().isEmpty())) {
            if (userList.contains(User.of(auth).getEmail().toLowerCase())) {
                return false; //pop-up modal
            } else {
                return true; //no modal
            }
        }
        if (!catalogueBundle.getMetadata().getTerms().contains(User.of(auth).getEmail().toLowerCase()) && userList.contains(User.of(auth).getEmail().toLowerCase())) {
            return false; // pop-up modal
        }
        return true; // no modal
    }

    public void adminAcceptedTerms(String catalogueId, Authentication auth) {
        update(get(catalogueId), auth);
    }

    @Override
    @CacheEvict(value = CACHE_CATALOGUES, allEntries = true)
    public CatalogueBundle verifyCatalogue(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Catalogue state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Catalogue State!", status));
        }
        logger.trace("verifyCatalogue with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        CatalogueBundle catalogue = get(id);
        catalogue.setStatus(vocabularyService.get(status).getId());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        if (catalogue.getLoggingInfo() != null) {
            loggingInfoList = catalogue.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "approved catalogue":
                if (active == null) {
                    active = true;
                }
                catalogue.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                catalogue.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                catalogue.setLatestOnboardingInfo(loggingInfo);
                break;
            case "rejected catalogue":
                catalogue.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                catalogue.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                catalogue.setLatestOnboardingInfo(loggingInfo);
                break;
            default:
                break;
        }
        logger.info("Verifying Catalogue: {}", catalogue);
        return super.update(catalogue, auth);
    }

    @Override
    @CacheEvict(value = CACHE_CATALOGUES, allEntries = true)
    public CatalogueBundle publish(String catalogueId, Boolean active, Authentication auth) {
        CatalogueBundle catalogue = get(catalogueId);
        if ((catalogue.getStatus().equals(vocabularyService.get("pending catalogue").getId()) ||
                catalogue.getStatus().equals(vocabularyService.get("rejected catalogue").getId())) && !catalogue.isActive()){
            throw new ValidationException(String.format("You cannot activate this Catalogue, because it's Inactive with status = [%s]", catalogue.getStatus()));
        }
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        if (catalogue.getLoggingInfo() != null) {
            loggingInfoList = catalogue.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }

        if (active == null) {
            active = false;
        }
        if (active != null) {
            catalogue.setActive(active);
            if (!active) {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.DEACTIVATED.getKey());
                loggingInfoList.add(loggingInfo);
                catalogue.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                catalogue.setLatestUpdateInfo(loggingInfo);

            } else {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
                loggingInfoList.add(loggingInfo);
                catalogue.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                catalogue.setLatestUpdateInfo(loggingInfo);
            }
        }
        return super.update(catalogue, auth);
    }

    @Cacheable(value = CACHE_CATALOGUES)
    public List<Map<String, Object>> createQueryForCatalogueFilters (FacetFilter ff, String orderDirection, String orderField){
        String keyword = ff.getKeyword();
        Map<String, Object> order = ff.getOrderBy();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        String query;
        if (ff.getFilter().entrySet().isEmpty()){
            query = "SELECT catalogue_id FROM catalogue_view";
        } else{
            query = "SELECT catalogue_id FROM catalogue_view WHERE";
        }

        boolean firstTime = true;
        boolean hasStatus = false;
        for (Map.Entry<String, Object> entry : ff.getFilter().entrySet()) {
            in.addValue(entry.getKey(), entry.getValue());
            if (entry.getKey().equals("status")) {
                hasStatus = true;
                if (firstTime) {
                    query += String.format(" (status=%s)", entry.getValue().toString());
                    firstTime = false;
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR status='");
                }
            }
        }

        // keyword on search bar
        if (keyword != null && !keyword.equals("")){
            if (firstTime){
                query += String.format(" WHERE upper(CONCAT(%s))", columnsOfInterest) + " like '%" + String.format("%s", keyword.toUpperCase()) + "%'";
            } else{
                query += String.format(" AND upper(CONCAT(%s))", columnsOfInterest) + " like '%" + String.format("%s", keyword.toUpperCase()) + "%'";
            }
        }

        // order/orderField
        if (orderField !=null && !orderField.equals("")){
            query += String.format(" ORDER BY %s", orderField);
        }
        if (orderField == null){
            query += String.format(" ORDER BY name");
        }
        if (orderDirection !=null && !orderDirection.equals("")){
            query += String.format(" %s", orderDirection);
        }

        query = query.replaceAll("\\[", "'").replaceAll("\\]","'");
        return namedParameterJdbcTemplate.queryForList(query, in);
    }

    @Cacheable(value = CACHE_CATALOGUES)
    public Paging<CatalogueBundle> createCorrectQuantityFacets(List<CatalogueBundle> catalogueBundle, Paging<CatalogueBundle> catalogueBundlePaging,
                                                              int quantity, int from){
        if (!catalogueBundle.isEmpty()) {
            List<CatalogueBundle> retWithCorrectQuantity = new ArrayList<>();
            if (from == 0){
                if (quantity <= catalogueBundle.size()){
                    for (int i=from; i<=quantity-1; i++){
                        retWithCorrectQuantity.add(catalogueBundle.get(i));
                    }
                } else{
                    retWithCorrectQuantity.addAll(catalogueBundle);
                }
                catalogueBundlePaging.setTo(retWithCorrectQuantity.size());
            } else{
                boolean indexOutOfBound = false;
                if (quantity <= catalogueBundle.size()){
                    for (int i=from; i<quantity+from; i++){
                        try{
                            retWithCorrectQuantity.add(catalogueBundle.get(i));
                            if (quantity+from > catalogueBundle.size()){
                                catalogueBundlePaging.setTo(catalogueBundle.size());
                            } else{
                                catalogueBundlePaging.setTo(quantity+from);
                            }
                        } catch (IndexOutOfBoundsException e){
                            indexOutOfBound = true;
                            continue;
                        }
                    }
                    if (indexOutOfBound){
                        catalogueBundlePaging.setTo(catalogueBundle.size());
                    }
                } else{
                    retWithCorrectQuantity.addAll(catalogueBundle);
                    if (quantity+from > catalogueBundle.size()){
                        catalogueBundlePaging.setTo(catalogueBundle.size());
                    } else{
                        catalogueBundlePaging.setTo(quantity+from);
                    }
                }
            }
            catalogueBundlePaging.setFrom(from);
            catalogueBundlePaging.setResults(retWithCorrectQuantity);
            catalogueBundlePaging.setTotal(catalogueBundle.size());
        } else{
            catalogueBundlePaging.setResults(catalogueBundle);
            catalogueBundlePaging.setTotal(0);
            catalogueBundlePaging.setFrom(0);
            catalogueBundlePaging.setTo(0);
        }
        return catalogueBundlePaging;
    }

    //SECTION: PROVIDER
    @Cacheable(value = CACHE_PROVIDERS)
    public ProviderBundle getCatalogueProvider(String catalogueId, String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        if (providerBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find provider with id: %s", providerId));
        }
        if (!providerBundle.getProvider().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Provider with id [%s] does not belong to the catalogue with id [%s]", providerId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsCatalogueAdmin(user, providerId)) {
                return providerBundle;
            }
        }
        // else return the Provider ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return providerBundle;
        }
        throw new ValidationException("You cannot view the specific Provider");
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle addCatalogueProvider(ProviderBundle provider, Authentication auth) {
        String catalogueId;
        if (provider.getProvider().getCatalogueId() == null || provider.getProvider().getCatalogueId().equals("")){
            throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
        } else{
            catalogueId = provider.getProvider().getCatalogueId();
        }
        try{
            get(catalogueId);
        } catch (ResourceNotFoundException e){
            logger.info(String.format("The catalogue with id [%s] does not exists.", catalogueId));
        }
        provider.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Provider: {} on Catalogue: {}", auth, provider, catalogueId);
        addAuthenticatedUser(provider.getProvider(), auth);
        providerService.validate(provider);
        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            providerService.validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()) {
            providerService.validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }
        providerService.validateEmailsAndPhoneNumbers(provider);
        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        provider.setLoggingInfo(loggingInfoList);
        provider.setActive(false);
        provider.setStatus(vocabularyService.get("pending provider").getId());
        provider.setTemplateStatus(vocabularyService.get("no template status").getId());

        // latestOnboardingInfo
        provider.setLatestOnboardingInfo(loggingInfo);

        if (provider.getProvider().getParticipatingCountries() != null && !provider.getProvider().getParticipatingCountries().isEmpty()){
            provider.getProvider().setParticipatingCountries(sortCountries(provider.getProvider().getParticipatingCountries()));
        }

        ProviderBundle ret;
        ret = providerResourceManager.add(provider, null);
        logger.debug("Adding Provider: {} of Catalogue: {}", provider, catalogueId);

        //TODO: ENABLE WHEN READY
//        registrationMailService.sendEmailsToNewlyAddedAdmins(provider, null);
//        jmsTopicTemplate.convertAndSend("provider.create", provider);
//        synchronizerServiceProvider.syncAdd(provider.getProvider());

        return ret;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle updateCatalogueProvider(ProviderBundle provider, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Provider with id '{}' of the Catalogue '{}'", auth, provider, provider.getProvider().getCatalogueId());
        providerService.validate(provider);
        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            providerService.validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()) {
            providerService.validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }
        providerService.validateEmailsAndPhoneNumbers(provider);
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
        ProviderBundle ex = providerResourceManager.deserialize(existing);
        provider.setActive(ex.isActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(providerResourceManager.serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Provider: {} of Catalogue {}", provider, provider.getProvider().getCatalogueId());

        // Send emails to newly added or deleted Admins
        providerService.adminDifferences(provider, ex);

        //TODO: ENABLE WHEN READY
        // send notification emails to Portal Admins
//        if (provider.getLatestAuditInfo() != null && provider.getLatestUpdateInfo() != null) {
//            Long latestAudit = Long.parseLong(provider.getLatestAuditInfo().getDate());
//            Long latestUpdate = Long.parseLong(provider.getLatestUpdateInfo().getDate());
//            if (latestAudit < latestUpdate && provider.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(provider);
//            }
//        }
//
//        jmsTopicTemplate.convertAndSend("provider.update", provider);
//
//        synchronizerServiceProvider.syncUpdate(provider.getProvider());

        return provider;
    }
}
