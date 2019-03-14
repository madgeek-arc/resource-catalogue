package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
    public ResponseEntity<Paging<Vocabulary>> get(@RequestParam Vocabulary.Types type) {
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
}
