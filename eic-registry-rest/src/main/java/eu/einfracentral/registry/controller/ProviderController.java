package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("provider")
@Api(value = "Get information about a Provider")
public class ProviderController extends ResourceController<Provider, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderController.class);
    private ProviderService<Provider, Authentication> providerManager;
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    ProviderController(ProviderService<Provider, Authentication> service,
                       InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(service);
        this.providerManager = service;
        this.infraServiceService = infraServiceService;
    }

    // Deletes the Provider with the given id.
    @RequestMapping(path = "{id}", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsProviderAdmin(#auth,#id)")
    public ResponseEntity<Provider> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Provider provider = providerManager.get(id);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting provider: " + provider.getName());
        providerManager.delete(provider);
        logger.info("User '{}' deleted the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "Returns the Provider with the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Provider provider = providerManager.get(id, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Creates a new Provider.
    @Override
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Provider> add(@RequestBody Provider provider, @ApiIgnore Authentication auth) {
        ResponseEntity<Provider> ret = super.add(provider, auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return ret;
    }

    @Override
    @ApiOperation(value = "Updates the Provider assigned the given id with the given Provider, keeping a version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsProviderAdmin(#auth,#provider.id)")
    public ResponseEntity<Provider> update(@RequestBody Provider provider, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<Provider> ret = super.update(provider, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return ret;
    }

    @Override
    @ApiOperation(value = "Filter a list of Providers based on a set of filters or get a list of all Providers in the eInfraCentral Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Provider>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
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
        return ResponseEntity.ok(providerManager.getAll(ff, auth));
    }

    @ApiOperation(value = "Get a list of services offered by a Provider.")
    @RequestMapping(path = "services/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getServices(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(providerManager.getServices(id), HttpStatus.OK);
    }

    // Get a featured InfraService offered by a Provider. // TODO enable in a future release
    @RequestMapping(path = "featured/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getFeaturedService(@PathVariable("id") String id) {
        return new ResponseEntity<>(providerManager.getFeaturedService(id), HttpStatus.OK);
    }

    // Get a list of Providers in which the given user is admin.
    @RequestMapping(path = "getServiceProviders", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Provider>> getServiceProviders(@RequestParam("email") String email, @ApiIgnore Authentication auth) {
        List<Provider> providers = providerManager.getServiceProviders(email, auth);
        if (providers == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

    // Get a list of Providers in which you are admin.
    @RequestMapping(path = "getMyServiceProviders", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Provider>> getMyServiceProviders(@ApiIgnore Authentication auth) {
        List<Provider> providers = providerManager.getMyServiceProviders(auth);
        if (providers == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

    // Get the pending services of the given Provider.
    @RequestMapping(path = "services/pending/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getInactiveServices(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        List<Service> ret = providerManager.getInactiveServices(id).stream().map(InfraService::getService).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Get all inactive Providers.
    @RequestMapping(path = "inactive/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Provider>> getInactive(@ApiIgnore Authentication auth) {
        List<Provider> ret = providerManager.getInactive();
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Accept/Reject a Provider.
    @RequestMapping(path = "verifyProvider/{id}", method = RequestMethod.PATCH, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Provider> verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                   @RequestParam(required = false) Provider.States status, @ApiIgnore Authentication auth) {
        Provider provider = providerManager.verifyProvider(id, status, active, auth);
        logger.info("User '{}' updated Provider with name '{}' [status: {}] [active: {}]", auth, provider.getName(), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Publish all Provider services.
    @RequestMapping(path = "publishServices", method = RequestMethod.PATCH, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<InfraService>> publishServices(@RequestParam String id, @RequestParam Boolean active,
                                                              @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Provider provider = providerManager.get(id);
        if (provider == null) {
            throw new ResourceException("Provider with id '" + id + "' does not exist.", HttpStatus.NOT_FOUND);
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("providers", id);
        List<InfraService> services = infraServiceService.getAll(ff, auth).getResults();
        for (InfraService service : services) {
            service.setActive(active);
//            service.setStatus(status.getKey());
            service.setLatest(active);
            Metadata metadata = service.getMetadata();
            metadata.setModifiedBy("system");
            metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            infraServiceService.update(service, auth);
            logger.info("User '{}' published(updated) all Services of the Provider with name '{}'",
                    auth.getName(), provider.getName());
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }


}
