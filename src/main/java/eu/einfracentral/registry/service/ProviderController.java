package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by pgl on 26/07/17.
 */
@RestController
@RequestMapping("provider")
public class ProviderController extends GenericRestController<Provider> {
    final private ProviderService providerService;

    @Autowired
    ProviderController(ProviderService service) {
        super(service);
        this.providerService = service;
    }
}