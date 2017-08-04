package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 4/7/2017.
 */
@RestController
@RequestMapping("service")
public class ServiceController extends GenericRestController<Service> {
    final private ServiceService serviceService;

    @Autowired
    ServiceController(ServiceService service) {
        super(service);
        this.serviceService = service;
    }


}