package eu.einfracentral.registry.controller;

import com.google.gson.Gson;
import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@RestController
@RequestMapping("public/")
@Api(value = "Get information about a published Provider")
public class PublicController {

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceBundleService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;
    private final ResourceService<ProviderBundle, Authentication> publicProviderManager;
    private final ResourceService<DatasourceBundle, Authentication> publicDatasourceManager;
    private final ResourceService<TrainingResourceBundle, Authentication> publicTrainingResourceManager;
    private final ResourceService<InteroperabilityRecordBundle, Authentication> publicInteroperabilityRecordManager;
    private final ResourceService<ResourceInteroperabilityRecordBundle, Authentication> publicResourceInteroperabilityRecordManager;
    private final SecurityService securityService;
    private final GenericResourceService genericResourceService;
    private static final Gson gson = new Gson();
    private static final Logger logger = LogManager.getLogger(PublicController.class);

    @Autowired
    PublicController(ProviderService<ProviderBundle, Authentication> providerService, SecurityService securityService,
                     ResourceBundleService<ServiceBundle> resourceBundleService,
                     ResourceBundleService<DatasourceBundle> datasourceBundleService,
                     TrainingResourceService<TrainingResourceBundle> trainingResourceBundleService,
                     InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                     ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                     @Qualifier("publicProviderManager") ResourceService<ProviderBundle, Authentication> publicProviderManager,
                     @Qualifier("publicDatasourceManager") ResourceService<DatasourceBundle, Authentication> publicDatasourceManager,
                     @Qualifier("publicTrainingResourceManager") ResourceService<TrainingResourceBundle, Authentication> publicTrainingResourceManager,
                     @Qualifier("publicInteroperabilityRecordManager") ResourceService<InteroperabilityRecordBundle, Authentication> publicInteroperabilityRecordManager,
                     @Qualifier("publicResourceInteroperabilityRecordManager") ResourceService<ResourceInteroperabilityRecordBundle, Authentication> publicResourceInteroperabilityRecordManager,
                     GenericResourceService genericResourceService) {
        this.providerService = providerService;
        this.resourceBundleService = resourceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceBundleService = trainingResourceBundleService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.publicProviderManager = publicProviderManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.securityService = securityService;
        this.genericResourceService = genericResourceService;
    }

    //SECTION: PROVIDER
    @ApiOperation(value = "Returns the Public Provider with the given id.")
    @GetMapping(path = "/provider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicProvider(@PathVariable("id") String id,
                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                               @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerService.get(catalogueId, id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, providerBundle)) {
                if (providerBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Provider does not consist a Public entity"));
                }
            }
        }
        if (providerBundle.getMetadata().isPublished() && providerBundle.isActive()
                && providerBundle.getStatus().equals("approved provider")) {
            return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Provider."));
    }

//    @ApiOperation(value = "Returns the Public Provider bundle with the given id.")
    @GetMapping(path = "/provider/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicProviderBundle(@PathVariable("id") String id,
                                                     @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                     @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerService.get(catalogueId, id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, providerBundle)) {
                if (providerBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(providerBundle, HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Provider Bundle does not consist a Public entity"));
                }
            }
        }
        if (providerBundle.getMetadata().isPublished() && providerBundle.isActive()
                && providerBundle.getStatus().equals("approved provider")) {
            return new ResponseEntity<>(providerBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Provider."));
    }

    @ApiOperation(value = "Filter a list of Public Providers based on a set of filters or get a list of all Public Providers in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/provider/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAllPublicProviders(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                   @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Providers for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved provider");
        }
        List<Provider> providerList = new LinkedList<>();
        Paging<ProviderBundle> providerBundlePaging = providerService.getAll(ff, auth);
        for (ProviderBundle providerBundle : providerBundlePaging.getResults()) {
            providerList.add(providerBundle.getProvider());
        }
        Paging<Provider> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/provider/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllPublicProviderBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                  @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Providers for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved provider");
        }
        Paging<ProviderBundle> providerBundlePaging = providerService.getAll(ff, auth);
        List<ProviderBundle> providerList = new LinkedList<>(providerBundlePaging.getResults());
        Paging<ProviderBundle> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @GetMapping(path = "/provider/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyPublicProviders(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicProviderManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    //SECTION: RESOURCE
    @ApiOperation(value = "Returns the Public Resource with the given id.")
    @GetMapping(path = "/resource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.resourceOrDatasourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicResource(@PathVariable("id") String id,
                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                               @ApiIgnore Authentication auth) {
        try{
            return resourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(resourceBundleService.get(id, catalogueId).getService(), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        } catch(eu.einfracentral.exception.ResourceNotFoundException e){
            return datasourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(datasourceBundleService.get(id, catalogueId).getDatasource(), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        }
    }

    //    @ApiOperation(value = "Returns the Public ServiceBundle with the given id.")
    @GetMapping(path = "/resource/infraService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicServiceBundle(@PathVariable("id") String id,
                                                    @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                    @ApiIgnore Authentication auth) {
        try{
            return resourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(resourceBundleService.get(id, catalogueId), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        } catch(eu.einfracentral.exception.ResourceNotFoundException e){
            return datasourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(datasourceBundleService.get(id, catalogueId), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Filter a list of Public Resources based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/resource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<?>> getAllPublicResources(@RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                 @RequestParam(defaultValue = "service", name = "type") String type,
                                                                 @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                 @ApiIgnore Authentication authentication) {
        FacetFilter ff =  resourceBundleService.createFacetFilterForFetchingServicesAndDatasources(allRequestParams, catalogueId, type);
        ff.getFilter().put("published", true);
        resourceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ResourceBundle<?>) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/resource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<?>> getAllPublicServiceBundles(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                            @RequestParam(defaultValue = "service", name = "type") String type,
                                                                            @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                            @ApiIgnore Authentication authentication) {
        FacetFilter ff = resourceBundleService.createFacetFilterForFetchingServicesAndDatasources(allRequestParams, catalogueId, type);
        ff.getFilter().put("published", true);
        resourceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "/resource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Object>> getMyPublicResources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.setResourceType("resources");
        ff.addOrderBy("name", "asc");
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        List<Object> resourceBundleList = new ArrayList<>();
        Paging<?> paging = genericResourceService.getResults(ff);
        for (Object o : paging.getResults()) {
            if (o instanceof ResourceBundle<?>){
                if (securityService.isResourceProviderAdmin(auth, ((ResourceBundle<?>) o).getId(), ((ResourceBundle<?>) o).getPayload().getCatalogueId()) && ((ResourceBundle<?>) o).getMetadata().isPublished()) {
                    resourceBundleList.add(o);
                }
            }
        }
        Browsing<Object> browsing = new Browsing<>(paging.getTotal(), paging.getFrom(), paging.getTo(), resourceBundleList, paging.getFacets());
        return new ResponseEntity<>(browsing.getResults(), HttpStatus.OK);
    }

    //SECTION: DATASOURCE
    @ApiOperation(value = "Returns the Public Datasource with the given id.")
    @GetMapping(path = "/datasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicDatasource(@PathVariable("id") String id,
                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                               @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, datasourceBundle.getPayload().getCatalogueId())) {
                if (datasourceBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Datasource does not consist a Public entity"));
                }
            }
        }
        if (datasourceBundle.getMetadata().isPublished() && datasourceBundle.isActive()
                && datasourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Datasource."));
    }

    @GetMapping(path = "/datasource/datasourceBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicDatasourceBundle(@PathVariable("id") String id,
                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                   @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, datasourceBundle.getPayload().getCatalogueId())) {
                if (datasourceBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(datasourceBundle, HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Datasource Bundle does not consist a Public entity"));
                }
            }
        }
        if (datasourceBundle.getMetadata().isPublished() && datasourceBundle.isActive()
                && datasourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(datasourceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Datasource."));
    }

    @ApiOperation(value = "Filter a list of Public Datasources based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/datasource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllPublicDatasources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                 @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                 @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Datasources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        List<Datasource> datasourceList = new LinkedList<>();
        Paging<DatasourceBundle> datasourceBundlePaging = publicDatasourceManager.getAll(ff, auth);
        for (DatasourceBundle datasourceBundle : datasourceBundlePaging.getResults()) {
            datasourceList.add(datasourceBundle.getDatasource());
        }
        Paging<Datasource> datasourcePaging = new Paging<>(datasourceBundlePaging.getTotal(), datasourceBundlePaging.getFrom(),
                datasourceBundlePaging.getTo(), datasourceList, datasourceBundlePaging.getFacets());
        return new ResponseEntity<>(datasourcePaging, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/datasource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllPublicDatasourceBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                           @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Datasources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        Paging<DatasourceBundle> datasourceBundlePaging = datasourceBundleService.getAll(ff, auth);
        List<DatasourceBundle> datasourceBundleList = new LinkedList<>(datasourceBundlePaging.getResults());
        Paging<DatasourceBundle> datasourcePaging = new Paging<>(datasourceBundlePaging.getTotal(), datasourceBundlePaging.getFrom(),
                datasourceBundlePaging.getTo(), datasourceBundleList, datasourceBundlePaging.getFacets());
        return new ResponseEntity<>(datasourcePaging, HttpStatus.OK);
    }

    @GetMapping(path = "/datasource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<DatasourceBundle>> getMyPublicDatasources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicDatasourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }


    //SECTION: TRAINING RESOURCE
    @ApiOperation(value = "Returns the Public Training Resource with the given id.")
    @GetMapping(path = "/trainingResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicTrainingResource(@PathVariable("id") String id,
                                                 @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                 @ApiIgnore Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, trainingResourceBundle.getTrainingResource().getCatalogueId())) {
                if (trainingResourceBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Training Resource does not consist a Public entity"));
                }
            }
        }
        if (trainingResourceBundle.getMetadata().isPublished() && trainingResourceBundle.isActive()
                && trainingResourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Training Resource."));
    }

    @GetMapping(path = "/trainingResource/trainingResourceBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicTrainingResourceBundle(@PathVariable("id") String id,
                                                        @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                        @ApiIgnore Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, trainingResourceBundle.getTrainingResource().getCatalogueId())) {
                if (trainingResourceBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Training Resource Bundle does not consist a Public entity"));
                }
            }
        }
        if (trainingResourceBundle.getMetadata().isPublished() && trainingResourceBundle.isActive()
                && trainingResourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Training Resource."));
    }

    @ApiOperation(value = "Filter a list of Public Training Resources based on a set of filters or get a list of all Public Training Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getAllPublicTrainingResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                      @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                      @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Training Resources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        List<TrainingResource> trainingResourceList = new LinkedList<>();
        Paging<TrainingResourceBundle> trainingResourceBundlePaging = publicTrainingResourceManager.getAll(ff, auth);
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBundlePaging.getResults()) {
            trainingResourceList.add(trainingResourceBundle.getTrainingResource());
        }
        Paging<TrainingResource> trainingResourcePaging = new Paging<>(trainingResourceBundlePaging.getTotal(), trainingResourceBundlePaging.getFrom(),
                trainingResourceBundlePaging.getTo(), trainingResourceList, trainingResourceBundlePaging.getFacets());
        return new ResponseEntity<>(trainingResourcePaging, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/trainingResource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllPublicTrainingResourceBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                                  @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Training Resources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        Paging<TrainingResourceBundle> trainingResourceBundlePaging = trainingResourceBundleService.getAll(ff, auth);
        List<TrainingResourceBundle> trainingResourceBundleList = new LinkedList<>(trainingResourceBundlePaging.getResults());
        Paging<TrainingResourceBundle> trainingResourcePaging = new Paging<>(trainingResourceBundlePaging.getTotal(), trainingResourceBundlePaging.getFrom(),
                trainingResourceBundlePaging.getTo(), trainingResourceBundleList, trainingResourceBundlePaging.getFacets());
        return new ResponseEntity<>(trainingResourcePaging, HttpStatus.OK);
    }

    @GetMapping(path = "/trainingResource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<TrainingResourceBundle>> getMyPublicTrainingResources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicTrainingResourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    //SECTION: INTEROPERABILITY RECORD
    @ApiOperation(value = "Returns the Public Interoperability Record with the given id.")
    @GetMapping(path = "/interoperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicInteroperabilityRecord(@PathVariable("id") String id,
                                                             @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId);
        if (interoperabilityRecordBundle.getMetadata().isPublished() && interoperabilityRecordBundle.isActive()
                && interoperabilityRecordBundle.getStatus().equals("approved interoperability record")) {
            return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Interoperability Record."));
    }

    @GetMapping(path = "/interoperabilityRecord/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicInteroperabilityRecordBundle(@PathVariable("id") String id,
                                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                   @ApiIgnore Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId())) {
                if (interoperabilityRecordBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Interoperability Record Bundle does not consist a Public entity"));
                }
            }
        }
        if (interoperabilityRecordBundle.getMetadata().isPublished() && interoperabilityRecordBundle.isActive()
                && interoperabilityRecordBundle.getStatus().equals("approved interoperability record")) {
            return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Interoperability Record."));
    }

    @ApiOperation(value = "Filter a list of Public Interoperability Records based on a set of filters or get a list of all Public Interoperability Records in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/interoperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getAllPublicInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                              @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                                              @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved interoperability record");
        List<InteroperabilityRecord> interoperabilityRecordList = new LinkedList<>();
        Paging<InteroperabilityRecordBundle> interoperabilityRecordBundlePaging = publicInteroperabilityRecordManager.getAll(ff, auth);
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundlePaging.getResults()) {
            interoperabilityRecordList.add(interoperabilityRecordBundle.getInteroperabilityRecord());
        }
        Paging<InteroperabilityRecord> interoperabilityRecordPaging = new Paging<>(interoperabilityRecordBundlePaging.getTotal(), interoperabilityRecordBundlePaging.getFrom(),
                interoperabilityRecordBundlePaging.getTo(), interoperabilityRecordList, interoperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(interoperabilityRecordPaging, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/interoperabilityRecord/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllPublicInteroperabilityRecordBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                          @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                                                          @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Interoperability Records for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved interoperability record");
        }
        Paging<InteroperabilityRecordBundle> interoperabilityRecordBundlePaging = interoperabilityRecordService.getAll(ff, auth);
        List<InteroperabilityRecordBundle> interoperabilityRecordBundleList = new LinkedList<>(interoperabilityRecordBundlePaging.getResults());
        Paging<InteroperabilityRecordBundle> interoperabilityRecordPaging = new Paging<>(interoperabilityRecordBundlePaging.getTotal(), interoperabilityRecordBundlePaging.getFrom(),
                interoperabilityRecordBundlePaging.getTo(), interoperabilityRecordBundleList, interoperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(interoperabilityRecordPaging, HttpStatus.OK);
    }

    @GetMapping(path = "/interoperabilityRecord/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<InteroperabilityRecordBundle>> getMyPublicInteroperabilityRecords(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("title", "asc");
        return new ResponseEntity<>(publicInteroperabilityRecordManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

//    //SECTION: RESOURCE INTEROPERABILITY RECORD
    @ApiOperation(value = "Returns the Public Resource Interoperability Record with the given id.")
    @GetMapping(path = "/resourceInteroperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicResourceInteroperabilityRecord(@PathVariable("id") String id) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(id);
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished() && resourceInteroperabilityRecordBundle.isActive()) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource Interoperability Record."));
    }

    @GetMapping(path = "/resourceInteroperabilityRecord/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicResourceInteroperabilityRecordBundle(@PathVariable("id") String id,
                                                                   @ApiIgnore Authentication auth) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId())) {
                if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()){
                    return new ResponseEntity<>(resourceInteroperabilityRecordBundle, HttpStatus.OK);
                } else{
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Resource Interoperability Record Bundle does not consist a Public entity"));
                }
            }
        }
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished() && resourceInteroperabilityRecordBundle.isActive()) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource Interoperability Record."));
    }

    @ApiOperation(value = "Filter a list of Public Resource Interoperability Records based on a set of filters or get a list of all Public Resource Interoperability Records in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/resourceInteroperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllPublicResourceInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                              @ApiIgnore Authentication auth) {

        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        List<ResourceInteroperabilityRecord> resourceInteroperabilityRecordList = new LinkedList<>();
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundlePaging = publicResourceInteroperabilityRecordManager.getAll(ff, auth);
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : resourceInteroperabilityRecordBundlePaging.getResults()) {
            resourceInteroperabilityRecordList.add(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord());
        }
        Paging<ResourceInteroperabilityRecord> resourceInteroperabilityRecordPaging = new Paging<>(resourceInteroperabilityRecordBundlePaging.getTotal(), resourceInteroperabilityRecordBundlePaging.getFrom(),
                resourceInteroperabilityRecordBundlePaging.getTo(), resourceInteroperabilityRecordList, resourceInteroperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(resourceInteroperabilityRecordPaging, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/resourceInteroperabilityRecord/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllPublicResourceInteroperabilityRecordBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                          @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Resource Interoperability Records for Admin/Epot");
        } else{
            ff.addFilter("active", true);
        }
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundlePaging = resourceInteroperabilityRecordService.getAll(ff, auth);
        List<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundleList = new LinkedList<>(resourceInteroperabilityRecordBundlePaging.getResults());
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordPaging = new Paging<>(resourceInteroperabilityRecordBundlePaging.getTotal(), resourceInteroperabilityRecordBundlePaging.getFrom(),
                resourceInteroperabilityRecordBundlePaging.getTo(), resourceInteroperabilityRecordBundleList, resourceInteroperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(resourceInteroperabilityRecordPaging, HttpStatus.OK);
    }

    @GetMapping(path = "/resourceInteroperabilityRecord/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ResourceInteroperabilityRecordBundle>> getMyPublicResourceInteroperabilityRecords(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("title", "asc");
        return new ResponseEntity<>(publicResourceInteroperabilityRecordManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
