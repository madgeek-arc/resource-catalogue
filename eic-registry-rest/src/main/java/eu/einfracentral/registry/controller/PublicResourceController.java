package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.InfraServiceService;
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
@RequestMapping("publicResource")
@Api(value = "Get information about a published Resource")
public class PublicResourceController {

    private final ResourceService<InfraService, Authentication> publicResourceManager;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final SecurityService securityService;
    private static final Logger logger = LogManager.getLogger(PublicResourceController.class);

    @Autowired
    PublicResourceController(@Qualifier("publicResourceManager") ResourceService<InfraService, Authentication> publicResourceManager,
                             InfraServiceService<InfraService, InfraService> infraServiceService, SecurityService securityService) {
        this.publicResourceManager = publicResourceManager;
        this.infraServiceService = infraServiceService;
        this.securityService = securityService;
    }

    @ApiOperation(value = "Returns the published Resource with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Service> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        InfraService infraService = infraServiceService.get(id);
        if (infraService.getStatus().equals("approved resource") && infraService.isActive() && infraService.isLatest() && infraService.getMetadata().isPublished()){
            return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
        } else{
            if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") ||
                    securityService.hasRole(auth, "ROLE_EPOT")) && infraService.getMetadata().isPublished()) {
                return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

//    @ApiOperation(value = "Returns the published InfraService with the given id.")
    @GetMapping(path = "/infraService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<InfraService> getInfraService(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        InfraService infraService = infraServiceService.get(id);
        if (infraService.getMetadata().isPublished()){
            return new ResponseEntity<>(infraService, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ApiOperation(value = "Filter a list of published Resources based on a set of filters or get a list of all published Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Service>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
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
            ff.addFilter("latest", true);
            ff.addFilter("status", "approved provider");
        }
        List<Service> serviceList = new LinkedList<>();
        Paging<InfraService> infraServicePaging = infraServiceService.getAll(ff, auth);
        for (InfraService infraService : infraServicePaging.getResults()) {
            serviceList.add(infraService.getService());
        }
        Paging<Service> servicePaging = new Paging<>(infraServicePaging.getTotal(), infraServicePaging.getFrom(),
                infraServicePaging.getTo(), serviceList, infraServicePaging.getFacets());
        return new ResponseEntity<>(servicePaging, HttpStatus.OK);
    }

    @GetMapping(path = "getMyPublishedResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<InfraService>> getMyPublishedResources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return new ResponseEntity<>(publicResourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}