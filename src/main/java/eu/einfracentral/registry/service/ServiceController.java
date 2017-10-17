package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by pgl on 4/7/2017.
 */
@RestController
@RequestMapping("service")
public class ServiceController extends GenericRestController<Service> {
    @Autowired
    ServiceController(ServiceService service) {
        super(service);
    }


}