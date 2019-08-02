package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
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

import java.util.*;

@RestController
@RequestMapping("vocabulary")
@Api(value = "Get information about the vocabularies")
public class VocabularyController extends ResourceController<Vocabulary, Authentication> {

    private VocabularyService vocabularyService;

    @Autowired
    VocabularyController(VocabularyService vocabularyService) {
        super(vocabularyService);
        this.vocabularyService = vocabularyService;
    }

    @ApiOperation(value = "Adds a new Vocabulary")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Vocabulary> add(@RequestBody Vocabulary vocabulary, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(vocabularyService.add(vocabulary, auth), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Adds all new Vocabularies")
    @RequestMapping(path = "/addAll", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public void addAll(@RequestBody List<Vocabulary> newVocabularies, @ApiIgnore Authentication auth) {
        vocabularyService.addAll(newVocabularies, auth);
    }

    @ApiOperation(value = "Delete All Vocs")
    @RequestMapping(path = "/deleteAll", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public void deleteAll(@ApiIgnore Authentication auth) {
        vocabularyService.deleteAll(auth);
    }

    @ApiOperation(value = "Updates a new Vocabulary")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Vocabulary> update(@RequestBody Vocabulary vocabulary, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return new ResponseEntity<>(vocabularyService.update(vocabulary, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Get a Map of vocabulary types and their respective entries")
    @GetMapping(path = "/byType", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<Vocabulary.Type, List<Vocabulary>>> getAllVocabulariesByType() {
        return new ResponseEntity<>(vocabularyService.getAllVocabulariesByType(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get vocabularies by type")
    @GetMapping(path = "/byType/{type}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Vocabulary>> getByType(@PathVariable(value = "type") Vocabulary.Type type) {
        return new ResponseEntity<>(vocabularyService.getByType(type), HttpStatus.OK);
    }

    @ApiOperation(value = "Delete a Vocabulary")
    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Vocabulary> delete(@RequestBody Vocabulary vocabulary, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(vocabularyService.del(vocabulary), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of EU countries.")
    @RequestMapping(path = "countries/EU", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(vocabularyService.getRegion("EU"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of WW countries.")
    @RequestMapping(path = "countries/WW", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getWW() {
        return new ResponseEntity<>(vocabularyService.getRegion("WW"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the entries of the specified Vocabulary type.")
    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> get(@RequestParam Vocabulary.Type type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", type.getKey());
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Vocabularies based on a set of filters or get a list of all Vocabularies in the eInfraCentral Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        Paging<Vocabulary> vocabularies = super.getAll(allRequestParams, auth).getBody();
        return new ResponseEntity<>(vocabularies, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a tree structure of Categories")
    @RequestMapping(path = "categoriesTree", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public @ResponseBody ResponseEntity<Map<Vocabulary, List<Map<Vocabulary, List<Vocabulary>>>>> getCategoriesTree() {
        Map<String, List<Vocabulary>> vocabularies = vocabularyService.getBy("parent_id");
        Map<Vocabulary, List<Map<Vocabulary, List<Vocabulary>>>> categoriesTree = new HashMap<>();
        for (Vocabulary supercategory : vocabularyService.getByType(Vocabulary.Type.SUPERCATEGORY)) {
            categoriesTree.put(supercategory, new ArrayList<>());
            for (Vocabulary category : vocabularies.get(supercategory.getId())) {
                Map<Vocabulary, List<Vocabulary>> categories = new HashMap<>();
                categories.put(category, new ArrayList<>());
                for (Vocabulary subcategory : vocabularies.get(category.getId())) {
                    categories.get(category).add(subcategory);
                }
                categoriesTree.get(supercategory).add(categories);
            }
        }
        return new ResponseEntity<>(categoriesTree, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a tree structure of Scientific Domains")
    @RequestMapping(path = "scientificDomainsTree", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Vocabulary>> getScientificDomainTree(@RequestParam Vocabulary.Type type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("vocabulary_id", type.getKey());
        return new ResponseEntity<>(vocabularyService.getAll(ff, null), HttpStatus.OK);
    }
}
