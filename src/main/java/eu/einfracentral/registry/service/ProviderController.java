package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
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