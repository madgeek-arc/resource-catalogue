package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pendingProvider")
public class PendingProviderController extends ResourceController<ProviderBundle, Authentication> {

    @Autowired
    PendingProviderController(ResourceService<ProviderBundle, Authentication> pendingProviderManager) {
        super(pendingProviderManager);
    }
}
