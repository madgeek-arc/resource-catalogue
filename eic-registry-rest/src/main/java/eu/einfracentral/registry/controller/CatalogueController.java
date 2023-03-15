package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@RestController
@RequestMapping("catalogue")
@Api(value = "Get information about a Catalogue")
public class CatalogueController {

    private static final Logger logger = LogManager.getLogger(CatalogueController.class);
    private final CatalogueService<CatalogueBundle, Authentication> catalogueManager;
    private final ProviderService<ProviderBundle, Authentication> providerManager;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final GenericResourceService genericResourceService;

    @Autowired
    CatalogueController(CatalogueService<CatalogueBundle, Authentication> catalogueManager,
                        ProviderService<ProviderBundle, Authentication> providerManager,
                        ResourceBundleService<ServiceBundle> resourceBundleService,
                        ResourceBundleService<DatasourceBundle> datasourceBundleService,
                        TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                        GenericResourceService genericResourceService) {
        this.catalogueManager = catalogueManager;
        this.providerManager = providerManager;
        this.resourceBundleService = resourceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.genericResourceService = genericResourceService;
    }

    //SECTION: CATALOGUE
    @ApiOperation(value = "Returns the Catalogue with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Catalogue> getCatalogue(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Catalogue catalogue = catalogueManager.get(id, auth).getCatalogue();
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Catalogue> addCatalogue(@RequestBody Catalogue catalogue, @ApiIgnore Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.add(new CatalogueBundle(catalogue), auth);
        logger.info("User '{}' added the Catalogue with name '{}' and id '{}'", auth.getName(), catalogue.getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> addCatalogueBundle(@RequestBody CatalogueBundle catalogue, @ApiIgnore Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.add(catalogue, auth);
        logger.info("User '{}' added the Catalogue with name '{}' and id '{}'", auth.getName(), catalogueBundle.getCatalogue().getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle, HttpStatus.CREATED);
    }

    //    @Override
    @ApiOperation(value = "Updates a specific Catalogue")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#catalogue.id)")
    public ResponseEntity<Catalogue> updateCatalogue(@RequestBody Catalogue catalogue, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        CatalogueBundle catalogueBundle = catalogueManager.get(catalogue.getId(), auth);
        catalogueBundle.setCatalogue(catalogue);
        if (comment == null || comment.equals("")) {
            comment = "no comment";
        }
        catalogueBundle = catalogueManager.update(catalogueBundle, comment, auth);
        logger.info("User '{}' updated the Catalogue with name '{}' and id '{}'", auth.getName(), catalogue.getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> updateCatalogueBundle(@RequestBody CatalogueBundle catalogue, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        CatalogueBundle catalogueBundle = catalogueManager.update(catalogue, auth);
        logger.info("User '{}' updated the Catalogue with name '{}' and id '{}'", auth.getName(), catalogueBundle.getCatalogue().getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Get a list of all Catalogues in the Portal.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Catalogue>> getAllCatalogues(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        List<Catalogue> catalogueList = new LinkedList<>();
        Paging<CatalogueBundle> catalogueBundlePaging = catalogueManager.getAll(ff, auth);
        for (CatalogueBundle catalogueBundle : catalogueBundlePaging.getResults()) {
            catalogueList.add(catalogueBundle.getCatalogue());
        }
        Paging<Catalogue> cataloguePaging = new Paging<>(catalogueBundlePaging.getTotal(), catalogueBundlePaging.getFrom(),
                catalogueBundlePaging.getTo(), catalogueList, catalogueBundlePaging.getFacets());
        return new ResponseEntity<>(cataloguePaging, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #id)")
    public ResponseEntity<CatalogueBundle> getCatalogueBundle(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(catalogueManager.get(id, auth), HttpStatus.OK);
    }

    // Get a list of Catalogues in which you are admin.
    @GetMapping(path = "getMyCatalogues", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<CatalogueBundle>> getMyCatalogues(@ApiIgnore Authentication auth) {
        return new ResponseEntity<>(catalogueManager.getMyCatalogues(auth), HttpStatus.OK);
    }

    // Accept/Reject a Catalogue.
    @PatchMapping(path = "verifyCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> verifyCatalogue(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                           @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.verifyCatalogue(id, status, active, auth);
        logger.info("User '{}' updated Catalogue with name '{}' [status: {}] [active: {}]", auth, catalogue.getCatalogue().getName(), status, active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    // Activate/Deactivate a Provider.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                   @ApiIgnore Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.publish(id, active, auth);
        logger.info("User '{}' updated Catalogue with name '{}' [status: {}] [active: {}]", auth, catalogue.getCatalogue().getName(), catalogue.getStatus(), active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    // Filter a list of Catalogues based on a set of filters or get a list of all Catalogues in the Portal.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getAllCatalogueBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth,
                                                                          @RequestParam(required = false) Set<String> status) {
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
        if (status != null) {
            ff.addFilter("status", status);
        }
        int quantity = ff.getQuantity();
        int from = ff.getFrom();
        Paging<CatalogueBundle> retPaging = catalogueManager.getAll(ff, auth);
        // FIXME: is the following needed?? remove
        List<Map<String, Object>> records = catalogueManager.createQueryForCatalogueFilters(ff, orderDirection, orderField);
        List<CatalogueBundle> ret = new ArrayList<>();
        for (Map<String, Object> record : records) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                ret.add(catalogueManager.get((String) entry.getValue()));
            }
        }
        return ResponseEntity.ok(catalogueManager.createCorrectQuantityFacets(ret, retPaging, quantity, from));
    }

    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String catalogueId, @ApiIgnore Authentication authentication) {
        return catalogueManager.hasAdminAcceptedTerms(catalogueId, authentication);
    }

    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void adminAcceptedTerms(@RequestParam String catalogueId, @ApiIgnore Authentication authentication) {
        catalogueManager.adminAcceptedTerms(catalogueId, authentication);
    }

    @ApiIgnore(value = "Deletes the Catalogue with the given id.")
//    @DeleteMapping(path = "delete/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Catalogue> deleteCatalogue(@PathVariable("id") String id,
                                                     @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        CatalogueBundle catalogueBundle = catalogueManager.get(id, auth);
        if (catalogueBundle == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catalogueManager.delete(catalogueBundle);
        logger.info("User '{}' deleted the Catalogue with id '{}' and name '{} along with all its related Resources'",
                auth.getName(), catalogueBundle.getCatalogue().getId(), catalogueBundle.getCatalogue().getName());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
    }

    //SECTION: PROVIDER
    @ApiOperation(value = "Returns the Provider of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/provider/{providerId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> getCatalogueProvider(@PathVariable("catalogueId") String catalogueId, @PathVariable("providerId") String providerId, @ApiIgnore Authentication auth) {
        Provider provider = providerManager.get(catalogueId, providerId, auth).getProvider();
        if (provider.getCatalogueId() == null) {
            throw new ValidationException("Provider's catalogueId cannot be null");
        } else {
            if (provider.getCatalogueId().equals(catalogueId)) {
                return new ResponseEntity<>(provider, HttpStatus.OK);
            } else {
                throw new ValidationException(String.format("The Provider [%s] you requested does not belong to the specific Catalogue [%s]", providerId, catalogueId));
            }
        }
    }

    @ApiOperation(value = "Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "{catalogueId}/provider/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAllCatalogueProviders(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @PathVariable("catalogueId") String catalogueId, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        if (!catalogueId.equals("all")) {
            ff.addFilter("catalogue_id", catalogueId);
        }
        List<Provider> providerList = new LinkedList<>();
        Paging<ProviderBundle> providerBundlePaging = providerManager.getAll(ff, auth);
        for (ProviderBundle providerBundle : providerBundlePaging.getResults()) {
            providerList.add(providerBundle.getProvider());
        }
        Paging<Provider> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Provider for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> addCatalogueProvider(@RequestBody Provider provider, @PathVariable String catalogueId, @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerManager.add(new ProviderBundle(provider), catalogueId, auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}' in the Catalogue '{}'", auth.getName(), provider.getName(), provider.getId(), catalogueId);
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.CREATED);
    }

    @PostMapping(path = "{catalogueId}/provider/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addCatalogueProviderBundle(@RequestBody ProviderBundle provider, @PathVariable String catalogueId, @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerManager.add(provider, catalogueId, auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}' in the Catalogue '{}'", auth.getName(), provider.getProvider().getName(), provider.getProvider().getId(), catalogueId);
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Provider of the specific Catalogue")
    @PutMapping(path = "{catalogueId}/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#provider.id, #provider.catalogueId)")
    public ResponseEntity<Provider> updateCatalogueProvider(@RequestBody Provider provider, @PathVariable String catalogueId, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerManager.get(catalogueId, provider.getId(), auth);
        providerBundle.setProvider(provider);
        if (comment == null || comment.equals("")) {
            comment = "no comment";
        }
        providerBundle = providerManager.update(providerBundle, comment, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{} of the Catalogue '{}'", auth.getName(), provider.getName(), provider.getId(), catalogueId);
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "{catalogueId}/provider/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateCatalogueProviderBundle(@RequestBody ProviderBundle provider, @PathVariable String catalogueId, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerManager.update(provider, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{} of the Catalogue '{}'", auth.getName(), provider.getProvider().getName(), provider.getProvider().getId(), catalogueId);
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes the Provider of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/provider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<Provider> deleteCatalogueProvider(@PathVariable("catalogueId") String catalogueId,
                                                            @PathVariable("id") String id,
                                                            @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerManager.get(catalogueId, id, auth);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        providerManager.delete(provider);
        logger.info("User '{}' deleted the Provider with name '{}' and id '{}'", auth.getName(), provider.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    //SECTION: Service
    @ApiOperation(value = "Returns the Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/resource/{resourceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getCatalogueService(@PathVariable("catalogueId") String catalogueId, @PathVariable("resourceId") String resourceId, @ApiIgnore Authentication auth) {
        try{
            return new ResponseEntity<>(resourceBundleService.get(resourceId, catalogueId).getService(), HttpStatus.OK);
        } catch(eu.einfracentral.exception.ResourceNotFoundException e){
            return new ResponseEntity<>(datasourceBundleService.get(resourceId, catalogueId).getDatasource(), HttpStatus.OK);
        }
    }

    @ApiOperation(value = "Creates a new Service for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<Service> addCatalogueService(@RequestBody Service service, @PathVariable String catalogueId, @ApiIgnore Authentication auth) {
        ServiceBundle ret = this.resourceBundleService.addResource(new ServiceBundle(service), catalogueId, auth);
        logger.info("User '{}' added the Service with name '{}' and id '{}' in the Catalogue '{}'", auth.getName(), service.getName(), service.getId(), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Service of the specific Catalogue.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#service)")
    @PutMapping(path = "{catalogueId}/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> updateCatalogueService(@RequestBody Service service, @PathVariable String catalogueId, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle ret = this.resourceBundleService.updateResource(new ServiceBundle(service), catalogueId, comment, auth);
        logger.info("User '{}' updated the Service with name '{}' and id '{} of the Catalogue '{}'", auth.getName(), service.getName(), service.getId(), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all the Services of a specific Provider of a specific Catalogue")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "{catalogueId}/{providerId}/resource/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<?>> getProviderServices(@PathVariable String catalogueId, @PathVariable String providerId,
                                                         @RequestParam(defaultValue = "service", name = "type") String type,
                                                         @ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff =  resourceBundleService.createFacetFilterForFetchingServicesAndDatasources(allRequestParams, catalogueId, type);
        ff.addFilter("resource_organisation", providerId);
        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ResourceBundle<?>) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @ApiOperation(value = "Deletes the Service of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/resource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<Service> deleteCatalogueService(@PathVariable("catalogueId") String catalogueId,
                                                          @PathVariable("id") String id,
                                                          @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(id, catalogueId);
        if (serviceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        resourceBundleService.delete(serviceBundle);
        logger.info("User '{}' deleted the Service with name '{}' and id '{}'", auth.getName(), serviceBundle.getService().getName(), serviceBundle.getId());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }


    //SECTION: DATASOURCE
    @ApiOperation(value = "Returns the Datasource of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/datasource/{resourceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Datasource> getCatalogueDatasource(@PathVariable("catalogueId") String catalogueId, @PathVariable("resourceId") String resourceId, @ApiIgnore Authentication auth) {
        Datasource datasource = datasourceBundleService.getCatalogueResource(catalogueId, resourceId, auth).getDatasource();
        if (datasource.getCatalogueId() == null) {
            throw new ValidationException("Datasource's catalogueId cannot be null");
        } else {
            if (datasource.getCatalogueId().equals(catalogueId)) {
                return new ResponseEntity<>(datasource, HttpStatus.OK);
            } else {
                throw new ValidationException(String.format("The Datasource [%s] you requested does not belong to the specific Catalogue [%s]", resourceId, catalogueId));
            }
        }
    }

    @ApiOperation(value = "Creates a new Datasource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasource)")
    public ResponseEntity<Datasource> addCatalogueDatasource(@RequestBody Datasource datasource, @PathVariable String catalogueId, @ApiIgnore Authentication auth) {
        DatasourceBundle ret = this.datasourceBundleService.addResource(new DatasourceBundle(datasource), catalogueId, auth);
        logger.info("User '{}' added the Datasource with name '{}' and id '{}' in the Catalogue '{}'", auth.getName(), datasource.getName(), datasource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Datasource of the specific Catalogue.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#datasource)")
    @PutMapping(path = "{catalogueId}/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> updateCatalogueDatasource(@RequestBody Datasource datasource, @PathVariable String catalogueId, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle ret = this.datasourceBundleService.updateResource(new DatasourceBundle(datasource), catalogueId, comment, auth);
        logger.info("User '{}' updated the Datasource with name '{}' and id '{} of the Catalogue '{}'", auth.getName(), datasource.getName(), datasource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all the Datasources of a specific Provider of a specific Catalogue")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "{catalogueId}/{providerId}/datasource/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<DatasourceBundle>> getProviderDatasources(@PathVariable String catalogueId, @PathVariable String providerId, @ApiIgnore Authentication auth) {
        Paging<DatasourceBundle> datasourceBundles = datasourceBundleService.getResourceBundles(catalogueId, providerId, auth);
        return new ResponseEntity<>(datasourceBundles, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes the Datasource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/datasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<Datasource> deleteCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                                @PathVariable("id") String id,
                                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasourceBundle = datasourceBundleService.get(id, catalogueId);
        if (datasourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        datasourceBundleService.delete(datasourceBundle);
        logger.info("User '{}' deleted the Datasource with name '{}' and id '{}'", auth.getName(), datasourceBundle.getDatasource().getName(), datasourceBundle.getId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    //SECTION: TRAINING RESOURCE
    @ApiOperation(value = "Returns the Training Resource of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/trainingResource/{resourceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<TrainingResource> getCatalogueTrainingResource(@PathVariable("catalogueId") String catalogueId, @PathVariable("resourceId") String resourceId, @ApiIgnore Authentication auth) {
        TrainingResource trainingResource = trainingResourceService.getCatalogueResource(catalogueId, resourceId, auth).getTrainingResource();
        if (trainingResource.getCatalogueId() == null) {
            throw new ValidationException("Training Resource's catalogueId cannot be null");
        } else {
            if (trainingResource.getCatalogueId().equals(catalogueId)) {
                return new ResponseEntity<>(trainingResource, HttpStatus.OK);
            } else {
                throw new ValidationException(String.format("The Training Resource [%s] you requested does not belong to the specific Catalogue [%s]", resourceId, catalogueId));
            }
        }
    }

    @ApiOperation(value = "Creates a new Training Resource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/trainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResource)")
    public ResponseEntity<TrainingResource> addCatalogueTrainingResource(@RequestBody TrainingResource trainingResource, @PathVariable String catalogueId, @ApiIgnore Authentication auth) {
        TrainingResourceBundle ret = this.trainingResourceService.addResource(new TrainingResourceBundle(trainingResource), catalogueId, auth);
        logger.info("User '{}' added the Training Resource with title '{}' and id '{}' in the Catalogue '{}'", auth.getName(), trainingResource.getTitle(), trainingResource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Training Resource of the specific Catalogue.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#trainingResource)")
    @PutMapping(path = "{catalogueId}/trainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<TrainingResource> updateCatalogueTrainingResource(@RequestBody TrainingResource trainingResource, @PathVariable String catalogueId, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        TrainingResourceBundle ret = this.trainingResourceService.updateResource(new TrainingResourceBundle(trainingResource), catalogueId, comment, auth);
        logger.info("User '{}' updated the Training Resource with title '{}' and id '{} of the Catalogue '{}'", auth.getName(), trainingResource.getTitle(), trainingResource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all the Training Resources of a specific Provider of a specific Catalogue")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<TrainingResourceBundle>> getProviderTrainingResources(@PathVariable String catalogueId, @PathVariable String providerId, @ApiIgnore Authentication auth) {
        Paging<TrainingResourceBundle> trainingResourceBundles = trainingResourceService.getResourceBundles(catalogueId, providerId, auth);
        return new ResponseEntity<>(trainingResourceBundles, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes the Training Resource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/trainingResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<TrainingResource> deleteCatalogueTrainingResource(@PathVariable("catalogueId") String catalogueId,
                                                                @PathVariable("id") String id,
                                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(id, catalogueId);
        if (trainingResourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        trainingResourceService.delete(trainingResourceBundle);
        logger.info("User '{}' deleted the Training Resource with title '{}' and id '{}'", auth.getName(), trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getId());
        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
    }
}
