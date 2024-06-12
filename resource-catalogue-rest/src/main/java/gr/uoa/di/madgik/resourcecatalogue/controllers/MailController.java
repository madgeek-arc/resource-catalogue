package gr.uoa.di.madgik.resourcecatalogue.controllers;

import com.google.common.collect.Lists;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping("mails")
@Tag(name = "mails", description = "Send emails to Users")
public class MailController {

    private static final Logger logger = LogManager.getLogger(MailController.class);

    @Autowired
    MailService mailService;
    @Autowired
    CatalogueService catalogueService;
    @Autowired
    ProviderService providerService;
    @Autowired
    DraftResourceService<ProviderBundle> pendingProviderService;
    @Autowired
    ServiceBundleService serviceBundleService;
    @Autowired
    DraftResourceService<ServiceBundle> pendingServiceService;
    @Autowired
    TrainingResourceService trainingResourceService;
    @Autowired
    SecurityService securityService;


    @PostMapping("custom")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void sendMails(@RequestParam(defaultValue = "") List<String> to,
                          @RequestParam(defaultValue = "") List<String> cc,
                          @RequestParam(defaultValue = "") List<String> bcc,
                          @RequestParam String subject, @RequestBody String text) throws MessagingException {
        mailService.sendMail(to, cc, bcc, subject, text);
    }

    @PostMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void sendToAll(@RequestParam(defaultValue = "") List<String> cc, @RequestParam String subject,
                          @RequestParam(defaultValue = "false") Boolean includeCatalogueAdmins,
                          @RequestParam(defaultValue = "false") Boolean includeProviderCatalogueContacts,
                          @RequestParam(defaultValue = "false") Boolean includeResourceContacts,
                          @RequestBody String text) throws MessagingException {
        int partitionSize = 100;
        if (cc != null) {
            partitionSize -= cc.size();
        }
        List<String> allEmails = getAllEmails(includeCatalogueAdmins, includeProviderCatalogueContacts, includeResourceContacts);
        for (List<String> bccChunk : Lists.partition(allEmails, partitionSize)) {
            logger.info(String.format("Sending emails to: %s", String.join(", ", bccChunk)));
            mailService.sendMail(new ArrayList<>(), cc, bccChunk, subject, text);
        }
    }

    List<String> getAllEmails(Boolean includeCatalogueAdmins, Boolean includeProviderCatalogueContacts,
                              Boolean includeResourceContacts) {
        Set<String> emails = new HashSet<>();

        FacetFilter facetFilter = createFacetFilter(false);
        Authentication adminAccess = securityService.getAdminAccess();

        addEmailsFromProviders(emails, facetFilter, adminAccess, includeProviderCatalogueContacts);
        if (includeCatalogueAdmins != null && includeCatalogueAdmins) {
            addEmailsFromCatalogues(emails, createFacetFilter(true), adminAccess, includeProviderCatalogueContacts);
        }
        if (includeResourceContacts != null && includeResourceContacts) {
            addEmailsFromServices(emails, facetFilter, adminAccess);
            addEmailsFromTrainingResources(emails, facetFilter, adminAccess);
        }

        return emails.stream().sorted().collect(Collectors.toList());
    }

    private void addEmailsFromProviders(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess,
                                        Boolean includeProviderCatalogueContacts) {
        List<ProviderBundle> allProviders = providerService.getAll(facetFilter, adminAccess).getResults();
        allProviders.addAll(pendingProviderService.getAll(facetFilter, adminAccess).getResults());

        for (ProviderBundle providerBundle : allProviders) {
            emails.addAll(providerBundle.getProvider().getUsers().stream().map(User::getEmail).collect(Collectors.toSet()));
            if (includeProviderCatalogueContacts != null && includeProviderCatalogueContacts) {
                emails.add(providerBundle.getProvider().getMainContact().getEmail());
                emails.addAll(providerBundle.getProvider().getPublicContacts().stream().map(ProviderPublicContact::getEmail).collect(Collectors.toSet()));
            }
        }
    }

    private void addEmailsFromCatalogues(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess, boolean includeContacts) {
        List<CatalogueBundle> allCatalogues = catalogueService.getAll(facetFilter, adminAccess).getResults();
        for (CatalogueBundle catalogueBundle : allCatalogues) {
            emails.addAll(catalogueBundle.getCatalogue().getUsers().stream().map(User::getEmail).collect(Collectors.toSet()));
            if (includeContacts) {
                emails.add(catalogueBundle.getCatalogue().getMainContact().getEmail());
                emails.addAll(catalogueBundle.getCatalogue().getPublicContacts().stream().map(ProviderPublicContact::getEmail).collect(Collectors.toSet()));
            }
        }
    }

    private void addEmailsFromServices(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess) {
        List<ServiceBundle> allServices = serviceBundleService.getAll(facetFilter, adminAccess).getResults();
        allServices.addAll(pendingServiceService.getAll(facetFilter, adminAccess).getResults());
        for (ServiceBundle serviceBundle : allServices) {
            emails.add(serviceBundle.getService().getMainContact().getEmail());
            emails.addAll(serviceBundle.getService().getPublicContacts().stream().map(ServicePublicContact::getEmail).collect(Collectors.toSet()));
        }
    }

    private void addEmailsFromTrainingResources(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess) {
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceService.getAll(facetFilter, adminAccess).getResults()) {
            emails.add(trainingResourceBundle.getTrainingResource().getContact().getEmail());
        }
    }

    private FacetFilter createFacetFilter(boolean isCatalogue) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        if (!isCatalogue) {
            ff.addFilter("published", false);
        }
        return ff;
    }
}
