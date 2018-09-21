package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.Browsing;
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
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
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
    @RequestMapping(path = "getEU", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(vocabularyService.getRegion("EU"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of WW countries.")
    @RequestMapping(path = "getWW", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getWW() {
        return new ResponseEntity<>(vocabularyService.getRegion("WW"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of languages.")
    @RequestMapping(path = "languages", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<Vocabulary>> getLanguages() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", "Language");
        return new ResponseEntity<>(vocabularyService.getAll(ff,null), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of categories.")
    @RequestMapping(path = "categories", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<Vocabulary>> getCategories() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", "Category");
        return new ResponseEntity<>(vocabularyService.getAll(ff,null), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the list of subcategories.")
    @RequestMapping(path = "subcategories", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<Vocabulary>> getSubcategories() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", "Subcategory");
        return new ResponseEntity<>(vocabularyService.getAll(ff,null), HttpStatus.OK);
    }

    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Vocabulary> get(@PathVariable("id") String id, Authentication jwt) throws ResourceNotFoundException {
        return super.get(id, jwt);
    }

    @ApiOperation(value = "Get all categories \\ sub categories used in eInfraCentral, etc.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of vocabularies to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, Authentication authentication) throws ResourceNotFoundException {
        return super.getAll(allRequestParams, authentication);
    }

    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Vocabulary>> getSome(@PathVariable String[] ids, Authentication jwt) {
        return super.getSome(ids, jwt);
    }

    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<Vocabulary>>> getBy(@PathVariable String field, Authentication jwt) {
        return super.getBy(field, jwt);
    }
}
