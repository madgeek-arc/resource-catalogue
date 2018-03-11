package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ServiceAddenda;
import eu.einfracentral.registry.service.ServiceAddendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("serviceAddenda")
public class ServiceAddendaController extends ResourceController<ServiceAddenda> {
    @Autowired
    ServiceAddendaController(ServiceAddendaService service) {
        super(service);
    }
}
