package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping("resourceInteroperabilityRecord")
@Tag(name = "resource interoperability record")
public class ResourceInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordController.class);

    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final GenericResourceService genericResourceService;

    public ResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                                    GenericResourceService genericResourceService) {
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the ResourceInteroperabilityRecord with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecord(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecord resourceInteroperabilityRecord = resourceInteroperabilityRecordService.get(id).getResourceInteroperabilityRecord();
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of ResourceInteroperabilityRecords based on a set of filters or get a list of all ResourceInteroperabilityRecords in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllResourceInteroperabilityRecords(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", false);
        Paging<ResourceInteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((ResourceInteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllResourceInteroperabilityRecordBundles(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", false);
        Paging<ResourceInteroperabilityRecordBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns the ResourceInteroperabilityRecord of the given Resource of the given Catalogue.")
    @GetMapping(path = "/byResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getResourceInteroperabilityRecordByResourceId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.
                getWithResourceId(id);
        if (resourceInteroperabilityRecordBundle != null) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Creates a new ResourceInteroperabilityRecord.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceInteroperabilityRecord.resourceId, #resourceInteroperabilityRecord.catalogueId)")
    public ResponseEntity<ResourceInteroperabilityRecord> addResourceInteroperabilityRecord(@RequestBody ResourceInteroperabilityRecord resourceInteroperabilityRecord,
                                                                                            @RequestParam String resourceType, @Parameter(hidden = true) Authentication auth) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.add(new ResourceInteroperabilityRecordBundle(resourceInteroperabilityRecord), resourceType, auth);
        logger.info("Added the ResourceInteroperabilityRecord with id '{}'", resourceInteroperabilityRecord.getId());
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
        logger.info("Updated the ResourceInteroperabilityRecord with id '{}'", resourceInteroperabilityRecordBundle.getId());
        return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{resourceIdPrefix}/{resourceIdSuffix}/{resourceInteroperabilityRecordIdPrefix}/{resourceInteroperabilityRecordIdSuffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #resourceIdPrefix+'/'+resourceIdSuffix)")
    public ResponseEntity<ResourceInteroperabilityRecord> deleteResourceInteroperabilityRecordById(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("resourceIdPrefix") String resourceIdPrefix,
                                                                                                   @Parameter(description = "The right part of the ID after the '/'") @PathVariable("resourceIdSuffix") String resourceIdSuffix,
                                                                                                   @Parameter(description = "The left part of the ID before the '/'") @PathVariable("resourceInteroperabilityRecordIdPrefix") String resourceInteroperabilityRecordIdPrefix,
                                                                                                   @Parameter(description = "The right part of the ID after the '/'") @PathVariable("resourceInteroperabilityRecordIdSuffix") String resourceInteroperabilityRecordIdSuffix,
                                                                                                   @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String resourceInteroperabilityRecordId = resourceInteroperabilityRecordIdPrefix + "/" + resourceInteroperabilityRecordIdSuffix;
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(resourceInteroperabilityRecordId);
        if (resourceInteroperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting ResourceInteroperabilityRecord: {} of the Catalogue: {}", resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId());
        resourceInteroperabilityRecordService.delete(resourceInteroperabilityRecordBundle);
        logger.info("Deleted the ResourceInteroperabilityRecord with id '{}' of the Catalogue '{}'",
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(), resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId());
        return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceInteroperabilityRecordBundle> getResourceInteroperabilityRecordBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
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

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordList, @Parameter(hidden = true) Authentication auth) {
        resourceInteroperabilityRecordService.addBulk(resourceInteroperabilityRecordList, auth);
    }
}
