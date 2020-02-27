package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.EventService;
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
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("provider")
@Api(value = "Get information about a Provider")
public class ProviderController {

    private static final Logger logger = LogManager.getLogger(ProviderController.class);
    private ProviderService<ProviderBundle, Authentication> providerManager;
    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private EventService eventService;

    @Autowired
    ProviderController(ProviderService<ProviderBundle, Authentication> service,
                       InfraServiceService<InfraService, InfraService> infraServiceService,
                       EventService eventService) {
        this.providerManager = service;
        this.infraServiceService = infraServiceService;
        this.eventService = eventService;
    }

    // Deletes the Provider with the given id.
    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Provider> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerManager.get(id);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting provider: {}", provider.getProvider().getName());
        providerManager.delete(provider);
        logger.info("User '{}' deleted the Provider with name '{}' and id '{}'", auth.getName(), provider.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Provider with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Provider provider = providerManager.get(id, auth).getProvider();
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Creates a new Provider.
//    @Override
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> add(@RequestBody Provider provider, @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerManager.add(new ProviderBundle(provider), auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addBundle(@RequestBody ProviderBundle provider, @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerManager.add(provider, auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}'", auth.getName(), providerBundle.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    //    @Override
    @ApiOperation(value = "Updates the Provider assigned the given id with the given Provider, keeping a version of revisions.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsProviderAdmin(#auth,#provider.id)")
    public ResponseEntity<Provider> update(@RequestBody Provider provider, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerManager.get(provider.getId(), auth);
        providerBundle.setProvider(provider);
        providerBundle = providerManager.update(providerBundle, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateBundle(@RequestBody ProviderBundle provider, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerManager.update(provider, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{}'", auth.getName(), providerBundle.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
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
        List<Provider> providerList = new LinkedList<>();
        Paging<ProviderBundle> providerBundlePaging = providerManager.getAll(ff, auth);
        for (ProviderBundle providerBundle : providerBundlePaging.getResults()) {
            providerList.add(providerBundle.getProvider());
        }
        Paging<Provider> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<ProviderBundle> getProviderBundle(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(providerManager.get(id, auth), HttpStatus.OK);
    }

    // Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<ProviderBundle>> getAllProviderBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
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
    @GetMapping(path = "services/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getServices(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(providerManager.getServices(id), HttpStatus.OK);
    }

    // Get a featured InfraService offered by a Provider. // TODO enable in a future release
    @GetMapping(path = "featured/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getFeaturedService(@PathVariable("id") String id) {
        return new ResponseEntity<>(providerManager.getFeaturedService(id), HttpStatus.OK);
    }

    // Get a list of Providers in which the given user is admin.
    @GetMapping(path = "getServiceProviders", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Provider>> getServiceProviders(@RequestParam("email") String email, @ApiIgnore Authentication auth) {
        List<Provider> providers = providerManager.getServiceProviders(email, auth)
                .stream()
                .map(ProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

    // Get a list of Providers in which you are admin.
    @GetMapping(path = "getMyServiceProviders", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyServiceProviders(@ApiIgnore Authentication auth) {
        return new ResponseEntity<>(providerManager.getMyServiceProviders(auth), HttpStatus.OK);
    }

    // Get the pending services of the given Provider.
    @GetMapping(path = "services/pending/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getInactiveServices(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        List<Service> ret = providerManager.getInactiveServices(id).stream().map(InfraService::getService).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Get all inactive Providers.
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Provider>> getInactive(@ApiIgnore Authentication auth) {
        List<Provider> ret = providerManager.getInactive()
                .stream()
                .map(ProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Accept/Reject a Provider.
    @PatchMapping(path = "verifyProvider/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) Provider.States status, @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerManager.verifyProvider(id, status, active, auth);
        logger.info("User '{}' updated Provider with name '{}' [status: {}] [active: {}]", auth, provider.getProvider().getName(), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Publish all Provider services.
    @PatchMapping(path = "publishServices", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<InfraService>> publishServices(@RequestParam String id, @RequestParam Boolean active,
                                                              @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle provider = providerManager.get(id);
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
                    auth.getName(), provider.getProvider().getName());
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @DeleteMapping(path = "/delete/userInfo", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public void deleteUserInfo (Authentication authentication){
        String userEmail = ((OIDCAuthenticationToken) authentication).getUserInfo().getEmail();
        String userId = ((OIDCAuthenticationToken) authentication).getUserInfo().getSub();
        List<Event> allUserFavorites = new ArrayList<>(eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), authentication));
//        List<Event> allUserRatings = new ArrayList<> (eventService.getUserEvents(Event.U0erActionType.RATING.getKey(), authentication));
        List<ProviderBundle> allUserProviders = new ArrayList<>(providerManager.getMyServiceProviders(authentication));
        for (ProviderBundle providerBundle : allUserProviders){
            List<User> updatedUsers = new ArrayList<>();
            if (providerBundle.getProvider().getUsers().size() > 1){
                eventService.deleteEvents(allUserFavorites);
//                eventService.deleteEvents(allUserRatings);
                for (User user : providerBundle.getProvider().getUsers()){
                    if (!user.getEmail().equals(userEmail)){
                        updatedUsers.add(user);
                    }
                }
                providerBundle.getProvider().setUsers(updatedUsers);
                try {
                    providerManager.update(providerBundle, authentication);
                } catch (ResourceNotFoundException e) {
                    logger.error(e);
                }
            }
        }
    }


}
