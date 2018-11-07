package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
@RequestMapping("vocabulary")
@Api(value = "Get auxiliary information about list of values (i.e., vocabularies) used in eInfraCentral")
public class VocabularyController extends ResourceController<Vocabulary, Authentication> {

    private VocabularyService vocabularyService;

    @Autowired
    VocabularyController(VocabularyService vocabulary) {
        super(vocabulary);
        this.vocabularyService = vocabulary;
    }

    @ApiOperation(value = "Returns the list of EU countries.")
    @RequestMapping(path = "countries/EU", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(vocabularyService.getRegion("EU"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of WW countries.")
    @RequestMapping(path = "countries/WW", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getWW() {
        return new ResponseEntity<>(vocabularyService.getRegion("WW"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of languages.")
    @RequestMapping(path = "languages", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getLanguages() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", "languages");
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of places.")
    @RequestMapping(path = "places", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getPlaces() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", "places");
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of categories.")
    @RequestMapping(path = "categories", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getCategories() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", "categories");
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of lifeCycleStatus.")
    @RequestMapping(path = "lifeCycleStatus", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getLifeCycleStatus() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", "lifecyclestatus");
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of TRL.")
    @RequestMapping(path = "TRL", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getTRL() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", "trl");
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Paging<Vocabulary> vocabularies = super.getAll(allRequestParams, auth).getBody();
        return new ResponseEntity<>(vocabularies, HttpStatus.OK);
    }
}
