package eu.openminted.registry.service;

import eu.openminted.registry.domain.Lexical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by stefanos on 13/1/2017.
 */
@RestController
@RequestMapping("/request/lexical")
public class LexicalController extends GenericRestController<Lexical> {

    @Autowired
    LexicalController(ResourceCRUDService<Lexical> service) {
        super(service);
    }
} 
