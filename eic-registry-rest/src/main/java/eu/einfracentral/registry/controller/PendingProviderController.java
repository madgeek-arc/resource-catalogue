package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.PendingResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pendingProvider")
public class PendingProviderController extends ResourceController<ProviderBundle, Authentication> {

    private final PendingResourceService<ProviderBundle> pendingProviderService;

    @Autowired
    PendingProviderController(PendingResourceService<ProviderBundle> pendingProviderService) {
        super(pendingProviderService);
        this.pendingProviderService = pendingProviderService;
    }

    @PostMapping("/transformToPending")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformProviderToPending(@RequestParam String providerId) {
        pendingProviderService.transformToPending(providerId);
    }

    @PostMapping("/transformToActive")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformProviderToActive(@RequestParam String providerId) {
        pendingProviderService.transformToActive(providerId);
    }

}
