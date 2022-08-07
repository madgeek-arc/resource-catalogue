package eu.einfracentral.registry.controller;

import com.google.gson.Gson;
import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@RestController
@RequestMapping("public/")
@Api(value = "Get information about a published Provider")
public class PublicController {

    private final ResourceService<ProviderBundle, Authentication> publicProviderManager;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ResourceService<ServiceBundle, Authentication> publicResourceServiceManager;
    private final ResourceService<DatasourceBundle, Authentication> publicResourceDatasourceManager;
    private final ResourceBundleService<ServiceBundle> resourceBundleServiceService;
    private final ResourceBundleService<DatasourceBundle> resourceBundleDatasourceService;
    private final SecurityService securityService;
    private static final Gson gson = new Gson();
    private static final Logger logger = LogManager.getLogger(PublicController.class);

    @Autowired
    PublicController(@Qualifier("publicProviderManager") ResourceService<ProviderBundle, Authentication> publicProviderManager,
                     ProviderService<ProviderBundle, Authentication> providerService, SecurityService securityService,
                     @Qualifier("publicResourceManager") ResourceService<ServiceBundle, Authentication> publicResourceServiceManager,
                     @Qualifier("publicDatasourceManager") ResourceService<DatasourceBundle, Authentication> publicResourceDatasourceManager,
                     ResourceBundleService<ServiceBundle> resourceBundleServiceService, ResourceBundleService<DatasourceBundle> resourceBundleDatasourceService) {
        this.publicProviderManager = publicProviderManager;
        this.providerService = providerService;
        this.securityService = securityService;
        this.publicResourceServiceManager = publicResourceServiceManager;
        this.publicResourceDatasourceManager = publicResourceDatasourceManager;
        this.resourceBundleServiceService = resourceBundleServiceService;
        this.resourceBundleDatasourceService = resourceBundleDatasourceService;
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
                    || securityService.userIsProviderAdmin(user, id)) {
                return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicProviderBundle(@PathVariable("id") String id,
                                                     @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                     @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerService.get(catalogueId, id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, id)) {
                return new ResponseEntity<>(providerBundle, HttpStatus.OK);
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
                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                   @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
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
                                                                  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                                  @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
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
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return new ResponseEntity<>(publicProviderManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    //SECTION: RESOURCE
    @ApiOperation(value = "Returns the Public Resource with the given id.")
    @GetMapping(path = "/resource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicResource(@PathVariable("id") String id,
                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                               @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = resourceBundleServiceService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id)) {
                return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
            }
        }
        if (serviceBundle.getMetadata().isPublished() && serviceBundle.isActive()
                && serviceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource."));
    }

    //    @ApiOperation(value = "Returns the Public ServiceBundle with the given id.")
    @GetMapping(path = "/resource/infraService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicInfraService(@PathVariable("id") String id,
                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                   @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = resourceBundleServiceService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id)) {
                return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
            }
        }
        if (serviceBundle.getMetadata().isPublished() && serviceBundle.isActive()
                && serviceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource."));
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
    public ResponseEntity<Paging<Service>> getAllPublicResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                  @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Resources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        List<Service> serviceList = new LinkedList<>();
        Paging<ServiceBundle> serviceBundlePaging = publicResourceServiceManager.getAll(ff, auth);
        for (ServiceBundle serviceBundle : serviceBundlePaging.getResults()) {
            serviceList.add(serviceBundle.getService());
        }
        Paging<Service> servicePaging = new Paging<>(serviceBundlePaging.getTotal(), serviceBundlePaging.getFrom(),
                serviceBundlePaging.getTo(), serviceList, serviceBundlePaging.getFacets());
        return new ResponseEntity<>(servicePaging, HttpStatus.OK);
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
    public ResponseEntity<Paging<ServiceBundle>> getAllPublicInfraServices(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                                           @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Resources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        Paging<ServiceBundle> serviceBundlePaging = resourceBundleServiceService.getAll(ff, auth);
        List<ServiceBundle> serviceList = new LinkedList<>(serviceBundlePaging.getResults());
        Paging<ServiceBundle> servicePaging = new Paging<>(serviceBundlePaging.getTotal(), serviceBundlePaging.getFrom(),
                serviceBundlePaging.getTo(), serviceList, serviceBundlePaging.getFacets());
        return new ResponseEntity<>(servicePaging, HttpStatus.OK);
    }

    @GetMapping(path = "/resource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ServiceBundle>> getMyPublicResources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return new ResponseEntity<>(publicResourceServiceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    //SECTION: DATASOURCE
    @ApiOperation(value = "Returns the Public Datasource with the given id.")
    @GetMapping(path = "/datasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicDatasource(@PathVariable("id") String id,
                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                               @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = resourceBundleDatasourceService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id)) {
                return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
            }
        }
        if (datasourceBundle.getMetadata().isPublished() && datasourceBundle.isActive()
                && datasourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Datasource."));
    }

    @GetMapping(path = "/datasource/datasourceBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicDatasourceBundles(@PathVariable("id") String id,
                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                   @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = resourceBundleDatasourceService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id)) {
                return new ResponseEntity<>(datasourceBundle, HttpStatus.OK);
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
                                                                 @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                                 @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Datasources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        List<Datasource> datasourceList = new LinkedList<>();
        Paging<DatasourceBundle> datasourceBundlePaging = publicResourceDatasourceManager.getAll(ff, auth);
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
                                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                                           @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Datasources for Admin/Epot");
        } else{
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        Paging<DatasourceBundle> datasourceBundlePaging = resourceBundleDatasourceService.getAll(ff, auth);
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
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return new ResponseEntity<>(publicResourceDatasourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
