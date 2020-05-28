package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.dto.VocabularyTree;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("vocabulary")
@Api(value = "Get information about the vocabularies")
public class VocabularyController extends ResourceController<Vocabulary, Authentication> {

    private final VocabularyService vocabularyService;

    @Autowired
    VocabularyController(VocabularyService vocabularyService) {
        super(vocabularyService);
        this.vocabularyService = vocabularyService;
    }

    /**
     * Open methods
     **/

    @ApiOperation(value = "Returns a list of EU countries.")
    @GetMapping(path = "countries/EU", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(vocabularyService.getRegion("EU"), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of WW countries.")
    @GetMapping(path = "countries/WW", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String[]> getWW() {
        return new ResponseEntity<>(vocabularyService.getRegion("WW"), HttpStatus.OK);
    }

    @ApiOperation(value = "Get by ID")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Vocabulary> get(@PathVariable("id") String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(vocabularyService.get(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a tree structure of Categories")
    @GetMapping(path = "vocabularyTree/{type}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<VocabularyTree> getVocabularyTree(@PathVariable("type") Vocabulary.Type type) {
        return new ResponseEntity<>(vocabularyService.getVocabulariesTree(type), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a map structure of vocabularies")
    @GetMapping(path = "vocabularyMap", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, Vocabulary>> getVocabularyMap() {
        return new ResponseEntity<>(vocabularyService.getVocabulariesMap(), HttpStatus.OK);
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


    /**
     * Secured methods
     **/

//    @ApiOperation(value = "Adds a new Vocabulary")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public ResponseEntity<Vocabulary> add(@RequestBody Vocabulary vocabulary, @ApiIgnore Authentication auth) {
        return super.add(vocabulary, auth);
    }

//    @ApiOperation(value = "Updates a Vocabulary")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public ResponseEntity<Vocabulary> update(@RequestBody Vocabulary vocabulary, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.update(vocabulary, auth);
    }

//    @ApiOperation(value = "Deletes a Vocabulary")
    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public ResponseEntity<Vocabulary> delete(@RequestBody Vocabulary vocabulary, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.delete(vocabulary, auth);
    }

//    @ApiOperation(value = "Adds all new Vocabularies")
    @PostMapping(path = "/addAll", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addAll(@RequestBody List<Vocabulary> newVocabularies, @ApiIgnore Authentication auth) {
        vocabularyService.addAll(newVocabularies, auth);
    }

//    @ApiOperation(value = "Delete All Vocs")
    @DeleteMapping(path = "/deleteAll", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteAll(@ApiIgnore Authentication auth) {
        vocabularyService.deleteAll(auth);
    }

    @ApiOperation(value = "Delete all Vocs of a specific type")
    @DeleteMapping(path = "/deleteByType/{type}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteByType(@PathVariable(value = "type") Vocabulary.Type type, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        List<Vocabulary> toBeDeleted = vocabularyService.getByType(type);
        for (Vocabulary vocabulary : toBeDeleted){
            delete(vocabulary, auth);
        }

    }
}
