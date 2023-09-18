package eu.einfracentral.registry.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@org.springframework.stereotype.Service
public class DatasourceManager extends ResourceManager<DatasourceBundle> implements DatasourceService<DatasourceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(DatasourceManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    @Value("${project.catalogue.name}")
    private String catalogueName;
    @Value("${openaire.dsm.api}")
    private String openaireAPI;

    @Autowired
    public DatasourceManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                             @Lazy SecurityService securityService,
                             @Lazy RegistrationMailService registrationMailService,
                             @Lazy VocabularyService vocabularyService,
                             ProviderResourcesCommonMethods commonMethods) {
        super(DatasourceBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "datasource";
    }

    public  DatasourceBundle get(String datasourceId) {
        Resource res = where(false, new SearchService.KeyValue("resource_internal_id", datasourceId));
        return res != null ? deserialize(res) : null;
    }

    public  DatasourceBundle get(String serviceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("service_id", serviceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public DatasourceBundle add(DatasourceBundle datasourceBundle, Authentication auth) {

        // if Datasource has ID -> check if it exists in OpenAIRE Datasources list
        if (datasourceBundle.getId() != null && !datasourceBundle.getId().equals("")){
            checkOpenAIREIDExistance(datasourceBundle);
        }
        datasourceBundle.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Datasource: {}", auth, datasourceBundle);

        this.validate(datasourceBundle);

        datasourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, auth);
        datasourceBundle.setLoggingInfo(loggingInfoList);
        differentiateInternalFromExternalCatalogueAddition(datasourceBundle);

        super.add(datasourceBundle, null);
        logger.debug("Adding Datasource for Service: {}", datasourceBundle.getDatasource().getServiceId());

        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueName)) {
            registrationMailService.sendEmailsForDatasourceExtension(datasourceBundle, "post");
        }
        return datasourceBundle;
    }

    private DatasourceBundle differentiateInternalFromExternalCatalogueAddition(DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueName)) {
            datasourceBundle.setActive(false);
            datasourceBundle.setStatus(vocabularyService.get("pending datasource").getId());
            datasourceBundle.setLatestOnboardingInfo(datasourceBundle.getLoggingInfo().get(0));
        } else {
            datasourceBundle.setActive(true);
            datasourceBundle.setStatus(vocabularyService.get("approved datasource").getId());
            LoggingInfo loggingInfo = commonMethods.createLoggingInfo(securityService.getAdminAccess(), LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            datasourceBundle.getLoggingInfo().add(loggingInfo);
            datasourceBundle.setLatestOnboardingInfo(datasourceBundle.getLoggingInfo().get(1));
        }
        return datasourceBundle;
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Datasource with id '{}'", auth, datasourceBundle.getId());

        Resource existing = whereID(datasourceBundle.getId(), true);
        DatasourceBundle ex = deserialize(existing);
        // check if there are actual changes in the Datasource
        if (datasourceBundle.getDatasource().equals(ex.getDatasource())) {
            return datasourceBundle;
        }

        super.validate(datasourceBundle);
        datasourceBundle.setMetadata(Metadata.updateMetadata(datasourceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        datasourceBundle.setLatestUpdateInfo(loggingInfo);
        datasourceBundle.setActive(ex.isActive());

        // if status = "rejected datasource", update to "pending datasource"
        if (ex.getStatus().equals(vocabularyService.get("rejected datasource").getId())) {
            datasourceBundle.setStatus(vocabularyService.get("pending datasource").getId());
        }

        // block user from updating serviceId
        if (!datasourceBundle.getDatasource().getServiceId().equals(ex.getDatasource().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Service Id with which this Datasource is related");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")){
            if (!ex.getDatasource().getCatalogueId().equals(datasourceBundle.getDatasource().getCatalogueId())){
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        existing.setPayload(serialize(datasourceBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
        logger.debug("Updating Datasource: {}", datasourceBundle);

        registrationMailService.sendEmailsForDatasourceExtension(datasourceBundle, "put");
        return datasourceBundle;
    }

    @Override
    public DatasourceBundle validate(DatasourceBundle datasourceBundle) {
        String serviceId = datasourceBundle.getDatasource().getServiceId();
        String catalogueId = datasourceBundle.getDatasource().getCatalogueId();

        DatasourceBundle existingDatasource = get(serviceId, catalogueId);
        if (existingDatasource != null) {
            throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Datasource " +
                    "registered, with id: [%s]", serviceId, catalogueId, existingDatasource.getId()));
        }

        // check if Service exists and if User belongs to Resource's Provider Admins
        ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(serviceId, catalogueId, serviceBundleService, "service");
        return super.validate(datasourceBundle);
    }

    public DatasourceBundle verifyDatasource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Datasource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist an Datasource state!", status));
        }
        logger.trace("Verifying Datasource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        DatasourceBundle datasourceBundle = get(id);

        // Verify that Service is Approved before proceeding
        if (!serviceBundleService.get(datasourceBundle.getDatasource().getServiceId(), datasourceBundle.getDatasource().getCatalogueId()).getStatus().equals("approved resource")
                && status.equals("approved datasource")) {
            throw new ValidationException("You cannot approve a Datasource when its Service is in Pending or Rejected state");
        }

        datasourceBundle.setStatus(vocabularyService.get(status).getId());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending datasource":
                break;
            case "approved datasource":
                datasourceBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                datasourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                datasourceBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            case "rejected datasource":
                datasourceBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                datasourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                datasourceBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            default:
                break;
        }

        logger.info("Verifying Datasource: {}", datasourceBundle);
        return super.update(datasourceBundle, auth);
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        super.delete(datasourceBundle);
        logger.debug("Deleting Datasource: {}", datasourceBundle);
    }

    public FacetFilter createFacetFilterForFetchingDatasources(MultiValueMap<String, Object> allRequestParams, String catalogueId){
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        allRequestParams.remove("catalogue_id");
        if (catalogueId != null){
            if (!catalogueId.equals("all")){
                ff.addFilter("catalogue_id", catalogueId);
            }
        }
        ff.addFilter("published", false);
        ff.setResourceType("datasource");
        return ff;
    }

    // OpenAIRE related methods
    private DatasourceBundle checkOpenAIREIDExistance(DatasourceBundle datasourceBundle){
        Datasource datasource = getOpenAIREDatasourceById(datasourceBundle.getId());
        if (datasource != null){
            createOpenAIREAlternativeIdentifiers(datasourceBundle);
        } else{
            throw new ValidationException(String.format("The ID [%s] you provided does not belong to an OpenAIRE Datasource", datasourceBundle.getId()));
        }
        return datasourceBundle;
    }

    public Datasource getOpenAIREDatasourceById(String openaireDatasourceID) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", openaireDatasourceID);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        if (datasource != null){
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    return transformOpenAIREToEOSCDatasource(jsonObj);
                }
            }
        }
        throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
    }

    private String[] getOpenAIREDatasourcesAsJSON(FacetFilter ff) {
        String[] pagination = createPagination(ff);
        int page = Integer.parseInt(pagination[0]);
        int quantity = Integer.parseInt(pagination[1]);
        String ordering = pagination[2];
        String data = pagination[3];
        String url = openaireAPI+"openaire/ds/searchdetails/"+page+"/"+quantity+"?order="+ordering+"&requestSortBy=id";
        String response = createHttpRequest(url, data);
        if (response != null){
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            String total = jsonObj.getAsJsonObject().get("header").getAsJsonObject().get("total").toString();
            jsonObj.getAsJsonObject().remove("header");
            return new String[]{total, jsonObj.toString()};
        }
        return new String[]{};
    }

    private String[] createPagination(FacetFilter ff){
        int page;
        int quantity = ff.getQuantity();
        if (ff.getFrom() >= quantity){
            page = ff.getFrom() / quantity;
        } else {
            page = ff.getFrom() / 10;
        }
        String ordering = "ASCENDING";
        if (ff.getOrderBy() != null){
            String order = ff.getOrderBy().get(ff.getOrderBy().keySet().toArray()[0]).toString();
            if (order.contains("desc")){
                ordering = "DESCENDING";
            }
        }
        String data = "{}";
        if (ff.getFilter() != null && !ff.getFilter().isEmpty()){
            page = 0;
            quantity = 10;
            if (ff.getFilter().containsKey("id")){
                data = "{  \"id\": \""+ff.getFilter().get("id")+"\"}";
            }
        }
        if (ff.getKeyword() != null && !ff.getKeyword().equals("")){
            data = "{  \"officialname\": \""+ff.getKeyword()+"\"}";
        }
        return new String[]{Integer.toString(page), Integer.toString(quantity), ordering, data};
    }

    private String createHttpRequest(String url, String data){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "application/json");
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(data, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }

    private DatasourceBundle createOpenAIREAlternativeIdentifiers(DatasourceBundle datasourceBundle) {
        Identifiers datasourceIdentifiers = new Identifiers();
        List<AlternativeIdentifier> datasourceAlternativeIdentifiers = new ArrayList<>();
        AlternativeIdentifier alternativeIdentifier = new AlternativeIdentifier();
        alternativeIdentifier.setType("openaire");
        alternativeIdentifier.setValue(datasourceBundle.getId());
        datasourceAlternativeIdentifiers.add(alternativeIdentifier);
        datasourceIdentifiers.setAlternativeIdentifiers(datasourceAlternativeIdentifiers);
        datasourceBundle.setIdentifiers(datasourceIdentifiers);
        return datasourceBundle;
    }

    public Map<Integer, List<Datasource>> getAllOpenAIREDatasources(FacetFilter ff) {
        Map<Integer, List<Datasource>> datasourceMap = new HashMap<>();
        List<Datasource> allDatasources = new ArrayList<>();
        String[] datasourcesAsJSON = getOpenAIREDatasourcesAsJSON(ff);
        int total = Integer.parseInt(datasourcesAsJSON[0]);
        String allOpenAIREDatasources = datasourcesAsJSON[1];
        if (allOpenAIREDatasources != null){
            JSONObject obj = new JSONObject(allOpenAIREDatasources);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            for(int i = 0; i < arr .length(); i++) {
                JSONObject map = arr.getJSONObject(i);
                Gson gson = new Gson();
                JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                Datasource datasource = transformOpenAIREToEOSCDatasource(jsonObj);
                if (datasource != null){
                    allDatasources.add(datasource);
                }
            }
            datasourceMap.put(total, allDatasources);
            return datasourceMap;
        }
        throw new ResourceNotFoundException("There are no OpenAIRE Datasources");
    }

    private Datasource transformOpenAIREToEOSCDatasource(JsonElement openaireDatasource){
        Datasource datasource = new Datasource();
        String id = openaireDatasource.getAsJsonObject().get("id").getAsString().replaceAll("\"", "");
        datasource.setId(id);
        return datasource;
    }

    public boolean isDatasourceRegisteredOnOpenAIRE(String eoscId){
        DatasourceBundle datasourceBundle = get(eoscId);
        boolean found = false;
        String registerBy;
        if (datasourceBundle != null){
            Identifiers identifiers = datasourceBundle.getIdentifiers();
            if (identifiers != null){
                List<AlternativeIdentifier> alternativeIdentifiers = identifiers.getAlternativeIdentifiers();
                if (alternativeIdentifiers != null && !alternativeIdentifiers.isEmpty()){
                    for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers){
                        if (alternativeIdentifier.getType().equals("openaire")){
                            registerBy = getOpenAIREDatasourceRegisterBy(alternativeIdentifier.getValue());
                            if(registerBy != null && !registerBy.equals("")){
                                found = true;
                            }
                        }
                    }
                }
            }
        } else{
            throw new ResourceNotFoundException(String.format("There is no Datasource with ID [%s]", eoscId));
        }
        return found;
    }

    private String getOpenAIREDatasourceRegisterBy(String openaireDatasourceID) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", openaireDatasourceID);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        String registerBy = null;
        if (datasource != null){
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    try {
                        registerBy = jsonObj.getAsJsonObject().get("registeredby").getAsString();
                    } catch (UnsupportedOperationException e) {
                        logger.error(e);
                    }
                }
            }
        }
        return registerBy;
    }

//    public DatasourceBundle createPublicResource(DatasourceBundle datasourceBundle, Authentication auth){
//        publicDatasourceManager.add(datasourceBundle, auth);
//        return datasourceBundle;
//    }

}
