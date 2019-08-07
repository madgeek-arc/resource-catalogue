package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("contact")
public class ContactController {

    private static final Logger logger = LogManager.getLogger(ContactController.class);

    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private ProviderService<Provider, Authentication> providerService;

    @Autowired
    ContactController(InfraServiceService<InfraService, InfraService> infraServiceService,
                      ProviderService<Provider, Authentication> providerService) {
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_PROVIDER', 'ROLE_USER')")
    @PostMapping("service/{ids}/support")
    public void sendRequest(@PathVariable("ids") List<String> ids, @RequestParam String email,
                            @RequestBody String message) {
        for (String id : ids) {
            List<String> emailsTo;
            InfraService service = infraServiceService.get(id);
            if (service.getSupportContact() != null && !service.getSupportContact().equals("")) {
                emailsTo = Collections.singletonList(service.getSupportContact());
            } else {
                emailsTo = service.getProviders()
                        .stream()
                        .map(providerId -> providerService.get(providerId).getContactEmail())
                        .collect(Collectors.toList());
            }

            logger.info(String.format("%nSending e-mail to '%s'%nFrom %s:%nMessage Body:%n%s", String.join(",", emailsTo), email, message));
        }
    }
}
