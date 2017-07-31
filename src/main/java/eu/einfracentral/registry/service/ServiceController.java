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

    @RequestMapping(value = "some/{ids}/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Service[]> getSome(@PathVariable String[] ids) {
        Service[] ret = serviceService.getSome(ids);
        if (ret == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(ret, HttpStatus.OK);
        }
    }
}