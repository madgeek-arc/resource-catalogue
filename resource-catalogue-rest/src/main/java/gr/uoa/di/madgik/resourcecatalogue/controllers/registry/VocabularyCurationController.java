package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.VocabularyCuration;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyCurationService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
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
import java.util.Set;

@Profile("beyond")
@RestController
@RequestMapping("vocabularyCuration")
@Tag(name = "vocabulary curation", description = "Operations about new Vocabulary suggestions")
public class VocabularyCurationController extends ResourceController<VocabularyCuration> {

    private static final Logger logger = LogManager.getLogger(VocabularyCurationController.class);
    private final VocabularyCurationService vocabularyCurationService;

    VocabularyCurationController(VocabularyCurationService service) {
        super(service);
        this.vocabularyCurationService = service;
    }

    //    @Operation(summary = "Get Vocabulary Curation by ID")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<VocabularyCuration> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(vocabularyCurationService.get(id), HttpStatus.OK);
    }

    @Override
//    @Operation(summary = "Creates a new Vocabulary Curation Request.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<VocabularyCuration> add(@RequestBody VocabularyCuration vocabularyCuration, @Parameter(hidden = true) Authentication auth) {
        ResponseEntity<VocabularyCuration> ret = super.add(vocabularyCuration, auth);
        logger.info("Adding new Vocabulary Curation");
        return ret;
    }

    //    @Operation(summary = "Creates a new Vocabulary Curation Request (front-end use).")
    @PostMapping(path = "addFront", produces = {MediaType.APPLICATION_JSON_VALUE})
    public VocabularyCuration addFront(@RequestParam(required = false) String resourceId, @RequestParam(required = false) String providerId,
                                       @RequestParam String resourceType, @RequestParam String entryValueName, @RequestParam String vocabulary,
                                       @RequestParam(required = false) String parent, @Parameter(hidden = true) Authentication auth) {
        logger.info("Adding new Vocabulary Curation (UI)");
        return vocabularyCurationService.addFront(resourceId, providerId, resourceType, entryValueName, vocabulary, parent, auth);
    }

    //    @Operation(summary = "Filter a list of Vocabulary Curation Requests based on a set of filters or get a list of all Vocabulary Curation Requests in the Catalogue.")
    @Browse
    @GetMapping(path = "vocabularyCurationRequests/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<VocabularyCuration>> getAllVocabularyCurationRequests(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Set<String> status,
                                                                                       @RequestParam(required = false) Set<String> vocabulary, @Parameter(hidden = true) Authentication authentication) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        return ResponseEntity.ok(vocabularyCurationService.getAllVocabularyCurationRequests(ff, authentication));
    }

    @PutMapping(path = "approveOrRejectVocabularyCuration", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void approveOrRejectVocabularyCuration(@RequestBody VocabularyCuration vocabularyCuration, @RequestParam boolean approved,
                                                  @RequestParam(required = false) String rejectionReason, @Parameter(hidden = true) Authentication authentication) {
        vocabularyCurationService.approveOrRejectVocabularyCuration(vocabularyCuration, approved, rejectionReason, authentication);
    }

    @Override
    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<VocabularyCuration> delete(@RequestBody VocabularyCuration vocabularyCuration,
                                                     @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        return super.delete(vocabularyCuration, auth);
    }

    @Override
    @DeleteMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<VocabularyCuration>> delAll(@Parameter(hidden = true) Authentication auth) {
        throw new UnsupportedOperationException("Not Implemented Yet!");
    }
}
