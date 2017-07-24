package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * Created by pgl on 24/7/2017.
 */
@RestController
@RequestMapping("vocabulary")
public class VocabularyController {
    @Autowired
    VocabularyService vocabularyService;

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public ResponseEntity<Vocabulary> getService(@PathVariable("id") String id) {
        String id_decoded = new String(Base64.getDecoder().decode(id));
        Vocabulary service = vocabularyService.get(id_decoded);
        if (service == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(service, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Browsing> getAllVocabularies() {
        FacetFilter filter = new FacetFilter();
        return new ResponseEntity<>(vocabularyService.getAll(filter), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> addVocabulary(@RequestBody Vocabulary vocabulary) {
        vocabularyService.add(vocabulary);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> addVocabularyXml(@RequestBody Vocabulary vocabulary) {
        vocabularyService.add(vocabulary);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity<String> updateVocabulary(@RequestBody Vocabulary vocabulary) {
        vocabularyService.update(vocabulary);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<String> deleteVocabulary(@RequestBody Vocabulary vocabulary) {
        vocabularyService.delete(vocabulary);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
