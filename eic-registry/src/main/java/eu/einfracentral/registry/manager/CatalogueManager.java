package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Service;


import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;

@Service("catalogueManager")
public class CatalogueManager extends ResourceManager<CatalogueBundle> implements CatalogueService<CatalogueBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(CatalogueManager.class);
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final IdCreator idCreator;
    private final JmsTemplate jmsTopicTemplate;
    private final FieldValidator fieldValidator;
    private final RegistrationMailService registrationMailService;
    private final DataSource dataSource;
    private final ProviderService<ProviderBundle, Authentication> providerManager;
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final String columnsOfInterest = "catalogue_id, name, abbreviation, affiliations, tags, networks," +
            "scientific_subdomains, hosting_legal_entity"; // variable with DB tables a keyword is been searched on

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public CatalogueManager(IdCreator idCreator, JmsTemplate jmsTopicTemplate, DataSource dataSource,
                            @Lazy ProviderService<ProviderBundle, Authentication> providerManager,
                            @Lazy ResourceBundleService<ServiceBundle> serviceBundleService,
                            @Lazy ResourceBundleService<DatasourceBundle> datasourceBundleService,
                            @Lazy FieldValidator fieldValidator,
                            @Lazy SecurityService securityService,
                            @Lazy VocabularyService vocabularyService,
                            @Lazy RegistrationMailService registrationMailService) {
        super(CatalogueBundle.class);
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.idCreator = idCreator;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.fieldValidator = fieldValidator;
        this.dataSource = dataSource;
        this.registrationMailService = registrationMailService;
        this.providerManager = providerManager;
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
    }

    @Override
    public String getResourceType() {
        return "catalogue";
    }

    @Override
    public CatalogueBundle get(String id) {
        CatalogueBundle catalogue = super.get(id);
        if (catalogue == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", id));
        }
        return catalogue;
    }

    @Override
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
        // else return the Catalogue ONLY if it is active
        if (catalogueBundle.getStatus().equals(vocabularyService.get("approved catalogue").getId())){
            return catalogueBundle;
        }
        throw new ValidationException("You cannot view the specific Catalogue");
    }

    @Override
    public void existsOrElseThrow(String id) {
        CatalogueBundle catalogueBundle = get(id);
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", id));
        }
    }

    @Override
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
    public CatalogueBundle add(CatalogueBundle catalogue, Authentication auth) {

        if (catalogue.getId() == null || catalogue.getId().equals("")){
            catalogue.setId(idCreator.createCatalogueId(catalogue.getCatalogue()));
        }
        logger.trace("User '{}' is attempting to add a new Catalogue: {}", auth, catalogue);
        addAuthenticatedUser(catalogue.getCatalogue(), auth);
        validate(catalogue);
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

        registrationMailService.sendEmailsToNewlyAddedCatalogueAdmins(catalogue, null);

        return ret;
    }

    public CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Catalogue with id '{}'", auth, catalogue);

        Resource existing = whereID(catalogue.getId(), true);
        CatalogueBundle ex = deserialize(existing);
        // check if there are actual changes in the Catalogue
        if (catalogue.getCatalogue().equals(ex.getCatalogue())){
            throw new ValidationException("There are no changes in the Catalogue", HttpStatus.OK);
        }

        validate(catalogue);
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

        catalogue.setActive(ex.isActive());
        catalogue.setStatus(ex.getStatus());
        existing.setPayload(serialize(catalogue));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Catalogue: {}", catalogue);

        // Send emails to newly added or deleted Admins
        adminDifferences(catalogue, ex);

        if (catalogue.getLatestAuditInfo() != null && catalogue.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(catalogue.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(catalogue.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && catalogue.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidCatalogueUpdate(catalogue);
            }
        }

        return catalogue;
    }

    @Override
    public CatalogueBundle validate(CatalogueBundle catalogue) {
        logger.debug("Validating Catalogue with id: {}", catalogue.getId());

        if (catalogue.getCatalogue().getScientificDomains() != null && !catalogue.getCatalogue().getScientificDomains().isEmpty()) {
            validateScientificDomains(catalogue.getCatalogue().getScientificDomains());
        }

        try {
            fieldValidator.validate(catalogue);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        return catalogue;
    }

    @Override
    public void delete(CatalogueBundle catalogueBundle) {
        String id = catalogueBundle.getId();

        // Block accidental deletion of main Catalogue
        if (id.equals(catalogueName)){
            throw new ValidationException(String.format("You cannot delete [%s] Catalogue.", catalogueName));
        }

        // Delete Catalogue along with all its related Resources
        logger.info("Deleting all Catalogue's Providers...");
        deleteCatalogueResources(id, providerManager, securityService.getAdminAccess());

        logger.info("Deleting all Catalogue's Services...");
        deleteCatalogueResources(id, serviceBundleService, securityService.getAdminAccess());

        logger.info("Deleting all Catalogue's Datasources...");
        deleteCatalogueResources(id, datasourceBundleService, securityService.getAdminAccess());

        logger.info("Deleting Catalogue...");
        super.delete(catalogueBundle);
    }

    @Override
    public List<CatalogueBundle> getMyCatalogues(Authentication auth) {
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        User user = User.of(auth);
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
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
        ff.addOrderBy("name", "asc");
        return getAll(ff, null).getResults();
    }

    @Override
    public <T, I extends ResourceCRUDService<T, Authentication>> void deleteCatalogueResources(String id, I service, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("catalogue_id", id);
        // Get all Catalogue's Resources
        List<T> allResources = service.getAll(ff, auth).getResults();
        for (T resource : allResources){
            try {
                logger.info("Deleting resource: {}", resource);
                service.delete(resource);
            } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
                logger.error(e);
            }
        }
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
        if (!adminsAdded.isEmpty()) {
            registrationMailService.sendEmailsToNewlyAddedCatalogueAdmins(updatedCatalogue, adminsAdded);
        }
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);
        if (!adminsDeleted.isEmpty()) {
            registrationMailService.sendEmailsToNewlyDeletedCatalogueAdmins(updatedCatalogue, adminsDeleted);
        }
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

        return super.update(catalogue, auth);
    }

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
                    for (int i=from; i<quantity+from; i++) {
                        try{
                            retWithCorrectQuantity.add(catalogueBundle.get(i));
                            if (quantity+from > catalogueBundle.size()){
                                catalogueBundlePaging.setTo(catalogueBundle.size());
                            } else{
                                catalogueBundlePaging.setTo(quantity+from);
                            }
                        } catch (IndexOutOfBoundsException e){
                            indexOutOfBound = true;
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

    public List<String> getAllCatalogueIds(){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<String> allCatalogueIds = getAll(ff, null).getResults().stream().map(CatalogueBundle::getId).collect(Collectors.toList());
        return allCatalogueIds;
    }
}
