package eu.openminted.registry.service;

import eu.openminted.registry.domain.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/request/component")
public class ComponentController extends GenericRestController<Component>{

    @Autowired
    ComponentController(ResourceCRUDService<Component> service) {
        super(service);
    }
}
