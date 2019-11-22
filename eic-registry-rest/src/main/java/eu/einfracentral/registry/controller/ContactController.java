package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.EmailMessage;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
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
    private ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    ContactController(InfraServiceService<InfraService, InfraService> infraServiceService,
                      ProviderService<ProviderBundle, Authentication> providerService) {
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("service/{ids}/support")
    public void sendRequest(@PathVariable("ids") List<String> ids, @RequestBody EmailMessage message) {
        for (String id : ids) {
            List<String> emailsTo;
            InfraService service = infraServiceService.get(id);
            if (service.getService().getContacts().get(0).getEmail() != null && !service.getService().getContacts().get(0).getEmail().equals("")) {
                emailsTo = Collections.singletonList(service.getService().getContacts().get(0).getEmail());
            } else {
                emailsTo = service.getService().getProviders()
                        .stream()
                        .map(providerId -> providerService.get(providerId).getProvider().getContacts().get(0).getEmail())
                        .collect(Collectors.toList());
            }
            // TODO: complete this method
            logger.info("\nSending e-mail to '{}'\nFrom {} <{}>:\nSubject: {}\nMessage Body:\n{}", String.join(",", emailsTo), message.getSenderName(), message.getSenderEmail(), message.getSubject(), message.getMessage());
        }
    }
}
