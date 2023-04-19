package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InteroperabilityRecordService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"interoperabilityRecord"})
@Api(description = "Operations for Interoperability Records")
public class InteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(InteroperabilityRecordController.class);
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;
    private final GenericResourceService genericResourceService;

    @Autowired
    public InteroperabilityRecordController(InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                                            ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                            GenericResourceService genericResourceService) {
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.genericResourceService = genericResourceService;
    }

    @ApiOperation(value = "Creates a new Interoperability Record.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddResources(#auth, #interoperabilityRecord)")
    public ResponseEntity<InteroperabilityRecord> add(@RequestBody InteroperabilityRecord interoperabilityRecord, @ApiIgnore Authentication auth) {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.add(new InteroperabilityRecordBundle(interoperabilityRecord), auth);
        logger.info("User '{}' added a new Interoperability Record with id '{}' and title '{}'", auth.getName(), interoperabilityRecord.getId(), interoperabilityRecord.getTitle());
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the InteroperabilityRecord with the given id.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isResourceProviderAdmin(#auth,#interoperabilityRecord)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InteroperabilityRecord> update(@Valid @RequestBody InteroperabilityRecord interoperabilityRecord,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.update(new InteroperabilityRecordBundle(interoperabilityRecord), auth);
        logger.info("User '{}' updated Interoperability Record with id '{}' and title '{}'", auth.getName(), interoperabilityRecord.getId(), interoperabilityRecord.getTitle());
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.OK);
    }

    // Deletes the Interoperability Record with the specific ID.
    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<InteroperabilityRecord> delete(@PathVariable("id") String id, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                         @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId);
        if (interoperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Interoperability Record: {}", interoperabilityRecordBundle.getId());
        interoperabilityRecordService.delete(interoperabilityRecordBundle);
        logger.info("User '{}' deleted the Interoperability Record with id '{}'", auth.getName(), interoperabilityRecordBundle.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Interoperability Record with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<InteroperabilityRecord> getInteroperabilityRecord(@PathVariable("id") String id,
                                                                            @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId) {
        InteroperabilityRecord interoperabilityRecord = interoperabilityRecordService.get(id, catalogueId).getInteroperabilityRecord();
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<InteroperabilityRecordBundle> getInteroperabilityRecordBundle(@PathVariable("id") String id,
                                                                                        @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                                        @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(interoperabilityRecordService.get(id, catalogueId), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all Interoperability Records")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getAll(@RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                 @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                 @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        ff.addFilter("status", "approved interoperability record");
        Paging<InteroperabilityRecordBundle> interoperabilityRecordPaging = interoperabilityRecordService.getAll(ff, auth);
        List<InteroperabilityRecord> interoperabilityRecords = interoperabilityRecordPaging.getResults().stream().map(InteroperabilityRecordBundle::getInteroperabilityRecord).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(interoperabilityRecordPaging.getTotal(), interoperabilityRecordPaging.getFrom(), interoperabilityRecordPaging.getTo(), interoperabilityRecords, interoperabilityRecordPaging.getFacets()));
    }

    @ApiOperation(value = "Get all Interoperability Record Bundles")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllBundles(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                              @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        allRequestParams.remove("catalogue_id");
        if (catalogueId != null){
            if (!catalogueId.equals("all")){
                ff.addFilter("catalogue_id", catalogueId);
            }
        }
        ff.addFilter("published", false);
        ff.setResourceType("interoperability_record");
        return ResponseEntity.ok(genericResourceService.getResults(ff));
    }

    @PatchMapping(path = "verify/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> verify(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                                         @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.verifyResource(id, status, active, auth);
        logger.info("User '{}' verified Interoperability Record with title '{}' [status: {}] [active: {}]", auth, interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), status, active);
        return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
    }

    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<InteroperabilityRecordBundle> setActive(@PathVariable String id, @RequestParam Boolean active, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to save Interoperability Record with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(interoperabilityRecordService.publish(id, active, auth));
    }

    @ApiOperation(value = "Validates the Interoperability Record without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody InteroperabilityRecord interoperabilityRecord) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(interoperabilityRecordService.validateInteroperabilityRecord(new InteroperabilityRecordBundle(interoperabilityRecord)));
        return ret;
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getInteroperabilityRecordsByProvider(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                         @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                                         @PathVariable String id, @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("provider_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(interoperabilityRecordService.getAll(ff, auth));
    }

    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,
                                                                  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId) {
        Paging<LoggingInfo> loggingInfoHistory = this.interoperabilityRecordService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @GetMapping(path = {"relatedResources/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getAllInteroperabilityRecordRelatedResources(@PathVariable String id) {
        List<String> allInteroperabilityRecordRelatedResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordService.getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds().contains(id)
                    && !resourceInteroperabilityRecordBundle.getMetadata().isPublished()){
                allInteroperabilityRecordRelatedResources.add(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId());
            }
        }
        return allInteroperabilityRecordRelatedResources;
    }

    @PostMapping(path = "addInteroperabilityRecordBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> add(@RequestBody InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        ResponseEntity<InteroperabilityRecordBundle> ret = new ResponseEntity<>(interoperabilityRecordService.add(interoperabilityRecordBundle, authentication), HttpStatus.OK);
        logger.info("User '{}' added InteroperabilityRecordBundle '{}' with id: {}", authentication, interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return ret;
    }

    @PutMapping(path = "updateInteroperabilityRecordBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> update(@RequestBody InteroperabilityRecordBundle interoperabilityRecordBundle, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<InteroperabilityRecordBundle> ret = new ResponseEntity<>(interoperabilityRecordService.update(interoperabilityRecordBundle, authentication), HttpStatus.OK);
        logger.info("User '{}' updated InteroperabilityRecordBundle '{}' with id: {}", authentication, interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return ret;
    }

    // Create a Public InteroperabilityRecord if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> createPublicInteroperabilityRecord(@RequestBody InteroperabilityRecordBundle interoperabilityRecordBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Interoperability Record from Interoperability Record '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), interoperabilityRecordBundle.getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
        return ResponseEntity.ok(interoperabilityRecordService.createPublicInteroperabilityRecord(interoperabilityRecordBundle, auth));
    }
}
