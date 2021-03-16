package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.VocabularyCuration;
import eu.einfracentral.registry.service.VocabularyCurationService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("vocabularyCuration")
@Api(value = "Get information about a Vocabulary Curation")
public class VocabularyCurationController extends ResourceController<VocabularyCuration, Authentication> {

    private static final Logger logger = LogManager.getLogger(VocabularyCurationController.class);
    private VocabularyCurationService<VocabularyCuration, Authentication> vocabularyCurationService;

    @Autowired
    VocabularyCurationController(VocabularyCurationService<VocabularyCuration, Authentication> service) {
        super(service);
        this.vocabularyCurationService = service;
    }

    @Override
    @ApiOperation(value = "Returns the Vocabulary Curation with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<VocabularyCuration> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @Override
    @ApiOperation(value = "Creates a new Vocabulary Curation Request.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<VocabularyCuration> add(@RequestBody VocabularyCuration vocabularyCuration, @ApiIgnore Authentication auth) {
        ResponseEntity<VocabularyCuration> ret = super.add(vocabularyCuration, auth);
        logger.info("asdf");
        return ret;
    }

    @ApiOperation(value = "Creates a new Vocabulary Curation Request (front-end use).")
    @PostMapping(path = "addFront", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void addFront(@RequestParam(required = false) String resourceId, @RequestParam(required = false) String providerId,
                         @RequestParam String resourceType, @RequestParam String entryValueName, @RequestParam String vocabulary,
                         @RequestParam(required = false) String parent, @ApiIgnore Authentication auth) {
        vocabularyCurationService.addFront(resourceId, providerId, resourceType, entryValueName, vocabulary, parent, auth);
    }

    @ApiOperation(value = "Filter a list of Vocabulary Curation Requests based on a set of filters or get a list of all Vocabulary Curation Requests in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "vocabularyCurationRequests/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Paging<VocabularyCuration>> getAllVocabularyCurationRequests(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                       @RequestParam(required = false) Set<String> status, @ApiIgnore Authentication authentication) {
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
        allRequestParams.remove("status");
        ff.setFilter(allRequestParams);
        if (status != null) {
            ff.addFilter("status", status);
        }
        return ResponseEntity.ok(vocabularyCurationService.getAllVocabularyCurationRequests(ff, authentication));
    }

    @PutMapping(path = "approveOrRejectVocabularyCuration", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void approveOrRejectVocabularyCuration(@RequestBody VocabularyCuration vocabularyCuration, @RequestParam boolean approved,
                                                  @RequestParam String rejectionReason, @ApiIgnore Authentication authentication){
        vocabularyCurationService.approveOrRejectVocabularyCuration(vocabularyCuration, approved, rejectionReason, authentication);
    }

}
