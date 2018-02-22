package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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
    @RequestMapping(value = "getEU", method = RequestMethod.GET)
    public ResponseEntity<String[]> getEU() {
        return new ResponseEntity<>(((VocabularyService) service).getEU(), HttpStatus.OK);
    }
}