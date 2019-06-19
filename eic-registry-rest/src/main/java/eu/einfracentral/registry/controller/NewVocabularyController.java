package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.NewVocabulary;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.NewVocabularyService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
@RequestMapping("newVocabulary")
@Api(value = "Get information about the vocabularies")
public class NewVocabularyController extends ResourceController<NewVocabulary, Authentication> {

    private NewVocabularyService newVocabularyService;

    @Autowired
    NewVocabularyController(NewVocabularyService newVocabularyService) {
        super(newVocabularyService);
        this.newVocabularyService = newVocabularyService;
    }

    @ApiOperation(value = "Adds a new Vocabulary")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<NewVocabulary> add(@RequestBody NewVocabulary newVocabulary, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(newVocabularyService.add(newVocabulary, auth), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates a new Vocabulary")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<NewVocabulary> update(@RequestBody NewVocabulary newVocabulary, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return new ResponseEntity<>(newVocabularyService.update(newVocabulary, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Convert vocabularies to new vocabularies")
    @PostMapping(path = "/convert", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<NewVocabulary>> convert() {
//        newVocabularyService.convertVocabularies();
        return new ResponseEntity<>(newVocabularyService.convertVocabularies(), HttpStatus.OK);
    }

    @ApiOperation(value = "Delete a Vocabulary")
    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<NewVocabulary> delete(@RequestBody NewVocabulary newVocabulary, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(newVocabularyService.del(newVocabulary), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of EU countries.")
    @RequestMapping(path = "countries/EU", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(newVocabularyService.getRegion("EU"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of WW countries.")
    @RequestMapping(path = "countries/WW", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getWW() {
        return new ResponseEntity<>(newVocabularyService.getRegion("WW"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the entries of the specified Vocabulary type.")
    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<NewVocabulary>> get(@RequestParam Vocabulary.Types type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", type.getKey());
        return new ResponseEntity<>(newVocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Vocabularies based on a set of filters or get a list of all Vocabularies in the eInfraCentral Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<NewVocabulary>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        Paging<NewVocabulary> vocabularies = super.getAll(allRequestParams, auth).getBody();
        return new ResponseEntity<>(vocabularies, HttpStatus.OK);
    }
}
