package gr.uoa.di.madgik.resourcecatalogue.controllers;

import com.google.common.collect.Lists;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.MailService;
import gr.uoa.di.madgik.resourcecatalogue.service.PendingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("mails")
public class MailController {

    private static final Logger logger = LogManager.getLogger(MailController.class);

    @Autowired
    MailService mailService;

    @Autowired
    ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    PendingResourceService<ProviderBundle> pendingProviderService;

    @Autowired
    ServiceBundleService<ServiceBundle> serviceBundleService;

    @Autowired
    SecurityService securityService;


    @PostMapping("custom")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void sendMails(@RequestParam(defaultValue = "") List<String> to, @RequestParam(defaultValue = "") List<String> cc,
                          @RequestParam(defaultValue = "") List<String> bcc, @RequestParam String subject,
                          @RequestBody String text) throws MessagingException {
        mailService.sendMail(to, cc, bcc, subject, text);
    }

    @PostMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void sendToAll(@RequestParam(defaultValue = "") List<String> cc, @RequestParam String subject,
                          @RequestBody String text) throws MessagingException {
        int partitionSize = 100;
        if (cc != null) {
            partitionSize -= cc.size();
        }
        List<String> allEmails = getAllEmails();
        for (List<String> bccChunk : Lists.partition(allEmails, partitionSize)) {
            logger.info(String.format("Sending emails to: %s", String.join(", ", bccChunk)));
            mailService.sendMail(new ArrayList<>(), cc, bccChunk, subject, text);
        }
    }

    List<String> getAllEmails() {
        Set<String> emails = new HashSet<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);

        List<ProviderBundle> allProviders = providerService.getAll(ff, securityService.getAdminAccess()).getResults();
        allProviders.addAll(pendingProviderService.getAll(ff, securityService.getAdminAccess()).getResults());

        for (ProviderBundle providerBundle : allProviders) {
            emails.add(providerBundle.getProvider().getMainContact().getEmail());
            emails.addAll(providerBundle.getProvider().getPublicContacts().stream().map(ProviderPublicContact::getEmail).collect(Collectors.toSet()));
            emails.addAll(providerBundle.getProvider().getUsers().stream().map(User::getEmail).collect(Collectors.toSet()));
        }

        ff = new FacetFilter();
        ff.setQuantity(10000);
        for (ServiceBundle bundle : serviceBundleService.getAll(ff, securityService.getAdminAccess()).getResults()) {
            emails.add(bundle.getService().getMainContact().getEmail());
            emails.addAll(bundle.getService().getPublicContacts().stream().map(ServicePublicContact::getEmail).collect(Collectors.toSet()));
        }

        return emails.stream().sorted().collect(Collectors.toList());
    }
}
