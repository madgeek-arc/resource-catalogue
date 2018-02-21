package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 24/7/2017.
 */
@RestController
@RequestMapping("vocabulary")
public class VocabularyController extends ResourceController<Vocabulary> {
    @Autowired
    VocabularyController(VocabularyService service) {
        super(service);
    }
}