package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ResourceInteroperabilityRecord;
import eu.einfracentral.domain.ResourceInteroperabilityRecordBundle;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("resourceInteroperabilityRecord")
@Api(value = "Operations for Resource Interoperability Records")
public class ResourceInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordController.class);

    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;

    public ResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService) {
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
    }

    @ApiOperation(value = "Returns the ResourceInteroperabilityRecord with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecord(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        ResourceInteroperabilityRecord resourceInteroperabilityRecord = resourceInteroperabilityRecordService.get(id).getResourceInteroperabilityRecord();
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
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllResourceInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                            @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                            @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        List<ResourceInteroperabilityRecord> resourceInteroperabilityRecordList = new LinkedList<>();
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundlePaging = resourceInteroperabilityRecordService.getAll(ff, auth);
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : resourceInteroperabilityRecordBundlePaging.getResults()) {
            resourceInteroperabilityRecordList.add(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord());
        }
        Paging<ResourceInteroperabilityRecord> resourceInteroperabilityRecordPaging = new Paging<>(resourceInteroperabilityRecordBundlePaging.getTotal(), resourceInteroperabilityRecordBundlePaging.getFrom(),
                resourceInteroperabilityRecordBundlePaging.getTo(), resourceInteroperabilityRecordList, resourceInteroperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(resourceInteroperabilityRecordPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the ResourceInteroperabilityRecord of the given Resource of the given Catalogue.")
    @GetMapping(path = "/byResource/{resourceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecordByResourceId(@PathVariable("resourceId") String resourceId,
                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                           @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordService.getAll(ff, auth).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId().equals(catalogueId)
                    && (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId().equals(resourceId))){
                return new ResponseEntity<>(resourceInteroperabilityRecord.getResourceInteroperabilityRecord(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new ResourceInteroperabilityRecord.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceInteroperabilityRecord.resourceId, #resourceInteroperabilityRecord.catalogueId)")
    public ResponseEntity<ResourceInteroperabilityRecord> addResourceInteroperabilityRecord(@Valid @RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                @RequestParam String resourceType, @ApiIgnore Authentication auth) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.add(new ResourceInteroperabilityRecordBundle(resourceInteroperabilityRecord), resourceType, auth);
        logger.info("User '{}' added the ResourceInteroperabilityRecord with id '{}'", auth.getName(), resourceInteroperabilityRecord.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the ResourceInteroperabilityRecord with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceInteroperabilityRecord.resourceId, #resourceInteroperabilityRecord.catalogueId)")
    public ResponseEntity<ResourceInteroperabilityRecord> updateResourceInteroperabilityRecord(@Valid @RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(resourceInteroperabilityRecord.getId());
        resourceInteroperabilityRecordBundle.setResourceInteroperabilityRecord(resourceInteroperabilityRecord);
        resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.update(resourceInteroperabilityRecordBundle, auth);
        logger.info("User '{}' updated the ResourceInteroperabilityRecord with id '{}'", auth.getName(), resourceInteroperabilityRecordBundle.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{resourceId}/{resourceInteroperabilityRecordId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceId)")
    public ResponseEntity<ResourceInteroperabilityRecord> deleteResourceInteroperabilityRecordById(@PathVariable("resourceId") String resourceId,
                                                                                                   @PathVariable("resourceInteroperabilityRecordId") String resourceInteroperabilityRecordId,
                                                                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(resourceInteroperabilityRecordId);
        if (resourceInteroperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting ResourceInteroperabilityRecord: {} of the Catalogue: {}", resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId());
        resourceInteroperabilityRecordService.delete(resourceInteroperabilityRecordBundle);
        logger.info("User '{}' deleted the ResourceInteroperabilityRecord with id '{}' of the Catalogue '{}'", auth.getName(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(), resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId());
        return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
    }
}
