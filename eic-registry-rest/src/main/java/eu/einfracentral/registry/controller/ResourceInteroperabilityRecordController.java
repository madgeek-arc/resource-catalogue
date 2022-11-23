package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ResourceInteroperabilityRecord;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("resourceInteroperabilityRecord")
@Api(value = "Get information about Resource Interoperability Record")
public class ResourceInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordController.class);

    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecord, Authentication> resourceInteroperabilityRecordService;

    public ResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService<ResourceInteroperabilityRecord, Authentication> resourceInteroperabilityRecordService) {
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
    }

    @ApiOperation(value = "Returns the ResourceInteroperabilityRecord with the given id.")
    @GetMapping(path = "/resourceInteroperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecord(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        ResourceInteroperabilityRecord resourceInteroperabilityRecord = resourceInteroperabilityRecordService.get(id);
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of ResourceInteroperabilityRecords based on a set of filters or get a list of all ResourceInteroperabilityRecords in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/resourceInteroperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllResourceInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
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
        Paging<ResourceInteroperabilityRecord> resourceInteroperabilityRecordPaging = resourceInteroperabilityRecordService.getAll(ff, auth);
        return new ResponseEntity<>(resourceInteroperabilityRecordPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new ResourceInteroperabilityRecord.")
    @PostMapping(path = "/resourceInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceInteroperabilityRecord.resourceId, #catalogueId)")
    public ResponseEntity<ResourceInteroperabilityRecord> addResourceInteroperabilityRecord(@Valid @RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                @RequestParam String resourceType, @ApiIgnore Authentication auth) {
        resourceInteroperabilityRecordService.add(resourceInteroperabilityRecord, resourceType, auth);
        logger.info("User '{}' added the ResourceInteroperabilityRecord with id '{}'", auth.getName(), resourceInteroperabilityRecord.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the ResourceInteroperabilityRecord with the given id.")
    @PutMapping(path = "/resourceInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceInteroperabilityRecord.resourceId, #catalogueId)")
    public ResponseEntity<ResourceInteroperabilityRecord> updateResourceInteroperabilityRecord(@Valid @RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        resourceInteroperabilityRecordService.update(resourceInteroperabilityRecord, auth);
        logger.info("User '{}' updated the ResourceInteroperabilityRecord with id '{}'", auth.getName(), resourceInteroperabilityRecord.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.OK);
    }

    @DeleteMapping(path = "/resourceInteroperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceInteroperabilityRecord> deleteResourceInteroperabilityRecordById(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResourceInteroperabilityRecord resourceInteroperabilityRecord = resourceInteroperabilityRecordService.get(id);
        if (resourceInteroperabilityRecord == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord.getId());
        resourceInteroperabilityRecordService.delete(resourceInteroperabilityRecord);
        logger.info("User '{}' deleted the ResourceInteroperabilityRecord with id '{}'", auth.getName(), resourceInteroperabilityRecord.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.OK);
    }
}
