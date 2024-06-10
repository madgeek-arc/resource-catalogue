package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Profile("crud")
@RestController
@RequestMapping({"interoperability-records"})
@Tag(name = "interoperability records")
public class InteroperabilityRecordCrudController {

    private static final Logger logger = LogManager.getLogger(InteroperabilityRecordCrudController.class);
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;

    public InteroperabilityRecordCrudController(InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService) {
        this.interoperabilityRecordService = interoperabilityRecordService;
    }


    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<InteroperabilityRecordBundle> get(@PathVariable("id") String id,
                                                            @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                            @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(interoperabilityRecordService.get(id, catalogueId), HttpStatus.OK);
    }

    @Browse
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> get(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                    @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(interoperabilityRecordService.getAll(FacetFilterUtils.createFacetFilter(allRequestParams), auth), HttpStatus.OK);
    }

    @Operation(summary = "Validates the Interoperability Record without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody InteroperabilityRecordBundle interoperabilityRecord) {
        return ResponseEntity.ok(interoperabilityRecordService.validateInteroperabilityRecord(interoperabilityRecord));
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> add(@RequestBody InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        ResponseEntity<InteroperabilityRecordBundle> ret = new ResponseEntity<>(interoperabilityRecordService.add(interoperabilityRecordBundle, authentication), HttpStatus.OK);
        logger.info("User '{}' added InteroperabilityRecordBundle '{}' with id: {}", authentication, interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return ret;
    }

    @PostMapping(path = "/bulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<InteroperabilityRecordBundle> interoperabilityRecordList, @Parameter(hidden = true) Authentication auth) {
        interoperabilityRecordService.addBulk(interoperabilityRecordList, auth);
    }

    @PutMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> update(@PathVariable String id, @RequestBody InteroperabilityRecordBundle interoperabilityRecord, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        if (!Objects.equals(id, interoperabilityRecord.getId())) {
            throw new ResourceException("Not the same resource", HttpStatus.CONFLICT);
        }
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.update(interoperabilityRecord, authentication);
        logger.info("User '{}' updated InteroperabilityRecordBundle '{}' with id: {}", authentication, interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<InteroperabilityRecordBundle> delete(@PathVariable("id") String id,
                                                               @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                               @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        interoperabilityRecordService.delete(interoperabilityRecordService.get(id, catalogueId));
        return new ResponseEntity<>(HttpStatus.OK);
    }


    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved interoperability record");
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        return ff;
    }
}