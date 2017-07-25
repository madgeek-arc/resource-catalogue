package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by pgl on 4/7/2017.
 */
@RestController
@RequestMapping("service")
public class ServiceController extends GenericRestController<Service> {

    final private ServiceService serviceService;

    @Autowired
    ServiceController(ServiceService serviceService) {
        super(serviceService);
        this.serviceService = serviceService;
    }
}
