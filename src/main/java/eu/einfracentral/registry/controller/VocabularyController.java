package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by pgl on 24/7/2017.
 */
@Api
@RestController
@RequestMapping("vocabulary")
public class VocabularyController extends ResourceController<Vocabulary> {
    @Autowired
    VocabularyController(VocabularyService service) { super(service); }

    @ApiOperation(value = "Returns the list of EU countries.")
    @RequestMapping(value = "getEU", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(((VocabularyService) service).getEU(), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ApiIgnore
    public ResponseEntity<Vocabulary> add(@RequestBody Vocabulary vocabulary, @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.add(vocabulary), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ApiIgnore
    public ResponseEntity<Vocabulary> update(@RequestBody Vocabulary vocabulary, @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return new ResponseEntity<>(service.update(vocabulary), HttpStatus.OK);
    }

    @RequestMapping(value = "validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ApiIgnore
    public ResponseEntity<Vocabulary> validate(@RequestBody Vocabulary vocabulary, @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return new ResponseEntity<>(service.validate(vocabulary), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ApiIgnore
    public ResponseEntity<Vocabulary> delete(@RequestBody Vocabulary vocabulary, @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.del(vocabulary), HttpStatus.OK);
    }

    @RequestMapping(path = "all", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ApiIgnore
    public ResponseEntity<List<Vocabulary>> delAll(@CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.delAll(), HttpStatus.OK);
    }
}
