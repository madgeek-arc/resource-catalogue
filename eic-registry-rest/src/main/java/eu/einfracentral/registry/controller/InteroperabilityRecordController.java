package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InteroperabilityRecord;
import eu.einfracentral.registry.service.ResourceService;
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

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"interoperabilityRecord"})
@Api(description = "Operations for Interoperability Records")
public class InteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(InteroperabilityRecordController.class);
    private final ResourceService<InteroperabilityRecord, Authentication> interoperabilityRecordService;

    @Autowired
    public InteroperabilityRecordController(ResourceService<InteroperabilityRecord, Authentication> interoperabilityRecordService) {
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @ApiOperation(value = "Returns the Interoperability Record with the given id.")
    @GetMapping(path = "/interoperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecord> getInteroperabilityRecord(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        InteroperabilityRecord interoperabilityRecord = interoperabilityRecordService.get(id);
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Interoperability Records based on a set of filters or get a list of all Interoperability Records in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/interoperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecord>> getAllInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                          @ApiIgnore Authentication auth) {
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
        Paging<InteroperabilityRecord> interoperabilityRecordPaging = interoperabilityRecordService.getAll(ff, auth);
        return new ResponseEntity<>(interoperabilityRecordPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Add a new Resource Interoperability Record")
    @PostMapping(path = "/interoperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecord> addInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord, @ApiIgnore Authentication auth) {
        interoperabilityRecordService.add(interoperabilityRecord, auth);
        logger.info("User '{}' added a new Interoperability Record with id '{}' and identifier '{}'", auth.getName(), interoperabilityRecord.getId(), interoperabilityRecord.getIdentifierInfo().getIdentifier());
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Interoperability Record with the given id.")
    @PutMapping(path = "/interoperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecord> updateHelpdesk(@Valid @RequestBody InteroperabilityRecord interoperabilityRecord,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        interoperabilityRecordService.update(interoperabilityRecord, auth);
        logger.info("User '{}' updated Interoperability Record with id '{}' and identifier '{}'", auth.getName(), interoperabilityRecord.getId(), interoperabilityRecord.getIdentifierInfo().getIdentifier());
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }

    // Deletes the Interoperability Record with the specific ID.
    @DeleteMapping(path = "/interoperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecord> deleteInteroperabilityRecordById(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InteroperabilityRecord interoperabilityRecord = interoperabilityRecordService.get(id);
        if (interoperabilityRecord == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Interoperability Record: {}", interoperabilityRecord.getId());
        interoperabilityRecordService.delete(interoperabilityRecord);
        logger.info("User '{}' deleted the Interoperability Record with id '{}'", auth.getName(), interoperabilityRecord.getId());
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }

}
