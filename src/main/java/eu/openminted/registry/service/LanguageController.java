package eu.openminted.registry.service;

import eu.openminted.registry.domain.LanguageDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by stefanos on 13/1/2017.
 */
@RestController
@RequestMapping("/request/language")
public class LanguageController extends GenericRestController<LanguageDescription> {

    @Autowired
    LanguageController(ResourceCRUDService<LanguageDescription> service) {
        super(service);
    }
} 
