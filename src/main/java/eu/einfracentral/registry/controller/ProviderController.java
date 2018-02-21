package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 26/07/17.
 */
@RestController
@RequestMapping("provider")
public class ProviderController extends ResourceController<Provider> {
    @Autowired
    ProviderController(ProviderService service) {
        super(service);
    }
}