/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.VocabularyTree;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("vocabulary")
@Tag(name = "vocabulary")
public class VocabularyController extends ResourceController<Vocabulary> {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyController.class);
    private final VocabularyService vocabularyService;

    VocabularyController(VocabularyService vocabularyService) {
        super(vocabularyService);
        this.vocabularyService = vocabularyService;
    }

    /**
     * Open methods
     **/

    @Operation(summary = "Returns a list of EU countries.")
    @GetMapping(path = "countries/EU", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(vocabularyService.getRegion("EU"), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of WW countries.")
    @GetMapping(path = "countries/WW", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String[]> getWW() {
        return new ResponseEntity<>(vocabularyService.getRegion("WW"), HttpStatus.OK);
    }

    @Operation(summary = "Get by ID")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Override
    public ResponseEntity<Vocabulary> get(@PathVariable("id") String id, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(vocabularyService.get(id), HttpStatus.OK);
    }

    @GetMapping(path = "vocabularyTree/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<VocabularyTree> getVocabularyTree(@PathVariable("type") Vocabulary.Type type) {
        return new ResponseEntity<>(vocabularyService.getVocabulariesTree(type), HttpStatus.OK);
    }

    @GetMapping(path = "vocabularyMap", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Vocabulary>> getVocabularyMap() {
        return new ResponseEntity<>(vocabularyService.getVocabulariesMap(), HttpStatus.OK);
    }

    @Operation(summary = "Get a Map of vocabulary types and their respective entries")
    @GetMapping(path = "/byType", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<Vocabulary.Type, List<Vocabulary>>> getAllVocabulariesByType() {
        return new ResponseEntity<>(vocabularyService.getAllVocabulariesByType(), HttpStatus.OK);
    }

    @Operation(summary = "Get vocabularies by type")
    @GetMapping(path = "/byType/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Vocabulary>> getByType(@PathVariable(value = "type") Vocabulary.Type type) {
        return new ResponseEntity<>(vocabularyService.getByType(type), HttpStatus.OK);
    }


    /**
     * Secured methods
     **/

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public ResponseEntity<Vocabulary> add(@RequestBody Vocabulary vocabulary, @Parameter(hidden = true) Authentication auth) {
        return super.add(vocabulary, auth);
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public ResponseEntity<Vocabulary> update(@RequestBody Vocabulary vocabulary, @Parameter(hidden = true) Authentication auth) {
        return super.update(vocabulary, auth);
    }

    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public ResponseEntity<Vocabulary> delete(@RequestBody Vocabulary vocabulary, @Parameter(hidden = true) Authentication auth) {
        return super.delete(vocabulary, auth);
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<Vocabulary> newVocabularies, @Parameter(hidden = true) Authentication auth) {
        vocabularyService.addBulk(newVocabularies, auth);
    }

    @PutMapping(path = "/updateBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void updateBulk(@RequestBody List<Vocabulary> vocabularies, @Parameter(hidden = true) Authentication auth) {
        vocabularyService.updateBulk(vocabularies, auth);
    }

    @DeleteMapping(path = "/deleteBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteBulk(@Parameter(hidden = true) Authentication auth) {
        vocabularyService.deleteAll(auth);
    }

    @DeleteMapping(path = "/deleteByType/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteByType(@PathVariable(value = "type") Vocabulary.Type type, @Parameter(hidden = true) Authentication auth) {
        List<Vocabulary> toBeDeleted = vocabularyService.getByType(type);
        for (Vocabulary vocabulary : toBeDeleted) {
            super.delete(vocabulary, auth);
        }
    }

    @GetMapping(path = "getSimilarHLEVocabularies", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void getSimilarHLEVocabularies() {
        List<Vocabulary> allHLE = vocabularyService.getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        List<String> allHLENames = new ArrayList<>();
        for (Vocabulary voc : allHLE) {
            allHLENames.add(voc.getName());
        }
        List<String> duplicateNames = allHLENames.stream()
                .filter(i -> Collections.frequency(allHLENames, i) > 1).distinct().toList();
        logger.info("Duplicate Names {}", duplicateNames);
    }

    // front-end use
    @Hidden
    @GetMapping(path = "getTerritories", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, List<Vocabulary>> getTerritories() {
        Map<String, List<Vocabulary>> territories = new HashMap<>();
        List<Vocabulary> vocs = new ArrayList<>();
        vocs.addAll(vocabularyService.getByType(Vocabulary.Type.REGION));
        vocs.addAll(vocabularyService.getByType(Vocabulary.Type.COUNTRY));
        territories.put("TERRITORIES", vocs);
        return territories;
    }
}
