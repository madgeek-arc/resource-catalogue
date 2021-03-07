package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.VocabularyCuration;
import eu.einfracentral.registry.service.VocabularyCurationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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
    public void addFront(@RequestParam String resourceId, @RequestParam String providerId, @RequestParam String resourceType,
                                                       @RequestParam String entryValueName, @RequestParam String vocabulary, @RequestParam String parent, @ApiIgnore Authentication auth) {
        vocabularyCurationService.addFront(resourceId, providerId, resourceType, entryValueName, vocabulary, parent, auth);
    }
}
