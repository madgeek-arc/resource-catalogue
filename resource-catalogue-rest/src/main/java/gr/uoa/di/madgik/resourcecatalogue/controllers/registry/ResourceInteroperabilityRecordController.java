package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;


import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("resourceInteroperabilityRecord")
@Tag(name = "resource interoperability record")
public class ResourceInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordController.class);

    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;

    public ResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService) {
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
    }

    @Operation(summary = "Returns the ResourceInteroperabilityRecord with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecord(@PathVariable("id") String id, @Parameter(hidden = true) Authentication auth) {
        ResourceInteroperabilityRecord resourceInteroperabilityRecord = resourceInteroperabilityRecordService.get(id).getResourceInteroperabilityRecord();
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of ResourceInteroperabilityRecords based on a set of filters or get a list of all ResourceInteroperabilityRecords in the Catalogue.")
    @Browse
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllResourceInteroperabilityRecords(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                                                        @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueIds,
                                                                                                        @Parameter(hidden = true) Authentication auth) {
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

    @Operation(summary = "Returns the ResourceInteroperabilityRecord of the given Resource of the given Catalogue.")
    @GetMapping(path = "/byResource/{resourceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecordByResourceId(@PathVariable("resourceId") String resourceId,
                                                                                                        @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.
                getWithResourceId(resourceId, catalogueId);
        if (resourceInteroperabilityRecordBundle != null) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Creates a new ResourceInteroperabilityRecord.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResourceInteroperabilityRecord> addResourceInteroperabilityRecord(@RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                                                            @RequestParam String resourceType, @Parameter(hidden = true) Authentication auth) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.add(new ResourceInteroperabilityRecordBundle(resourceInteroperabilityRecord), resourceType, auth);
        logger.info("User '{}' added the ResourceInteroperabilityRecord with id '{}'", auth.getName(), resourceInteroperabilityRecord.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the ResourceInteroperabilityRecord with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceInteroperabilityRecord.resourceId, #resourceInteroperabilityRecord.catalogueId)")
    public ResponseEntity<ResourceInteroperabilityRecord> updateResourceInteroperabilityRecord(@RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                                                               @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
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
                                                                                                   @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
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

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceInteroperabilityRecordBundle> getResourceInteroperabilityRecordBundle(@PathVariable("id") String id, @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        return new ResponseEntity<>(resourceInteroperabilityRecordService.get(id, catalogueId), HttpStatus.OK);
    }

    // Create a Public ResourceInteroperabilityRecord if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicResourceInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResourceInteroperabilityRecordBundle> createPublicResourceInteroperabilityRecord(@RequestBody ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Resource Interoperability Record from Resource Interoperability Record '{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), resourceInteroperabilityRecordBundle.getId(), resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId());
        return ResponseEntity.ok(resourceInteroperabilityRecordService.createPublicResourceInteroperabilityRecord(resourceInteroperabilityRecordBundle, auth));
    }
}
