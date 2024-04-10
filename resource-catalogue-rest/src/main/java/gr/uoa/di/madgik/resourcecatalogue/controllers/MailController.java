package gr.uoa.di.madgik.resourcecatalogue.controllers;

import com.google.common.collect.Lists;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
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
    CatalogueService<CatalogueBundle, Authentication> catalogueService;
    @Autowired
    ProviderService<ProviderBundle, Authentication> providerService;
    @Autowired
    PendingResourceService<ProviderBundle> pendingProviderService;
    @Autowired
    ServiceBundleService<ServiceBundle> serviceBundleService;
    @Autowired
    PendingResourceService<ServiceBundle> pendingServiceService;
    @Autowired
    TrainingResourceService<TrainingResourceBundle> trainingResourceService;
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
                          @RequestParam(defaultValue = "false") Boolean includeContacts,
                          @RequestBody String text) throws MessagingException {
        int partitionSize = 100;
        if (cc != null) {
            partitionSize -= cc.size();
        }
        List<String> allEmails = getAllEmails(includeCatalogueAdmins, includeContacts);
        for (List<String> bccChunk : Lists.partition(allEmails, partitionSize)) {
            logger.info(String.format("Sending emails to: %s", String.join(", ", bccChunk)));
            mailService.sendMail(new ArrayList<>(), cc, bccChunk, subject, text);
        }
    }

    List<String> getAllEmails(Boolean includeCatalogueAdmins, Boolean includeContacts) {
        Set<String> emails = new HashSet<>();

        boolean includeContactsFlag = includeContacts != null && includeContacts;
        FacetFilter facetFilter = createFacetFilter(); //TODO: test this
        Authentication adminAccess = securityService.getAdminAccess();

        addEmailsFromProviders(emails, facetFilter, adminAccess, includeContactsFlag);
        if (includeCatalogueAdmins != null && includeCatalogueAdmins) {
            addEmailsFromCatalogues(emails, facetFilter, adminAccess, includeContactsFlag);
        }
        if (includeContactsFlag) {
            addEmailsFromServices(emails, facetFilter, adminAccess);
            addEmailsFromTrainingResources(emails, facetFilter, adminAccess);
        }

        return emails.stream().sorted().collect(Collectors.toList());
    }

    private void addEmailsFromProviders(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess, boolean includeContacts) {
        List<ProviderBundle> allProviders = providerService.getAll(facetFilter, adminAccess).getResults();
        allProviders.addAll(pendingProviderService.getAll(facetFilter, adminAccess).getResults());

        for (ProviderBundle providerBundle : allProviders) {
            emails.addAll(providerBundle.getProvider().getUsers().stream().map(User::getEmail).collect(Collectors.toSet()));
            if (includeContacts) {
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

    private FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return ff;
    }
}
