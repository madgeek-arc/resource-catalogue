package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 24/7/2017.
 */
@RestController
@RequestMapping("vocabulary")
public class VocabularyController extends GenericRestController<Vocabulary> {
    final private VocabularyService vocabularyService;

    @Autowired
    VocabularyController(VocabularyService service) {
        super(service);
        this.vocabularyService = service;
    }

}
