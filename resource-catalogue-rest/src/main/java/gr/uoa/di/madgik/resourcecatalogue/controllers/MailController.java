/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers;

import com.google.common.collect.Lists;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.EmailUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(MailController.class);

    @Autowired
    MailService mailService;
    @Autowired
    OrganisationService organisationService;
    @Autowired
    ServiceService serviceService;
    @Autowired
    DatasourceService datasourceService;
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
                          @RequestParam(defaultValue = "false") Boolean includeContacts,
                          @RequestParam(defaultValue = "false") Boolean includeResourceContacts,
                          @RequestBody String text) throws MessagingException {
        int partitionSize = 100;
        if (cc != null) {
            partitionSize -= cc.size();
        }
        List<String> allEmails = getAllEmails(includeContacts, includeResourceContacts);
        for (List<String> bccChunk : Lists.partition(allEmails, partitionSize)) {
            logger.info("Sending emails to: {}", String.join(", ", bccChunk));
            mailService.sendMail(new ArrayList<>(), cc, bccChunk, subject, text);
        }
    }

    List<String> getAllEmails(Boolean includeContacts, Boolean includeResourceContacts) {
        Set<String> emails = new HashSet<>();
        Authentication adminAccess = securityService.getAdminAccess();

        addEmailsFromProviders(emails, createFacetFilter(), adminAccess, includeContacts);
        if (includeResourceContacts != null && includeResourceContacts) {
            //TODO: populate with more resources if needed
            addEmailsFromServices(emails, createFacetFilter(), adminAccess);
            addEmailsFromDatasources(emails, createFacetFilter(), adminAccess);
        }
        return emails.stream().sorted().collect(Collectors.toList());
    }

    private void addEmailsFromProviders(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess,
                                        Boolean includeContacts) {
        List<OrganisationBundle> allOrganisations = organisationService.getAll(facetFilter, adminAccess).getResults();
        for (OrganisationBundle organisationBundle : allOrganisations) {
            emails.addAll(EmailUtils.getUserEmails(organisationBundle.getOrganisation()));
            if (includeContacts != null && includeContacts) {
                emails.add(EmailUtils.getMainContactEmail(organisationBundle.getOrganisation()));
                emails.addAll(EmailUtils.getPublicContactEmails(organisationBundle.getOrganisation()));
            }
        }
    }

    private void addEmailsFromServices(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess) {
        List<ServiceBundle> allServices = serviceService.getAll(facetFilter, adminAccess).getResults();
        for (ServiceBundle serviceBundle : allServices) {
            emails.add(EmailUtils.getMainContactEmail(serviceBundle.getService()));
            emails.addAll(EmailUtils.getPublicContactEmails(serviceBundle.getService()));
        }
    }

    private void addEmailsFromDatasources(Set<String> emails, FacetFilter facetFilter, Authentication adminAccess) {
        List<DatasourceBundle> allDatasources = datasourceService.getAll(facetFilter, adminAccess).getResults();
        for (DatasourceBundle datasourceBundle : allDatasources) {
            emails.add(EmailUtils.getMainContactEmail(datasourceBundle.getDatasource()));
            emails.addAll(EmailUtils.getPublicContactEmails(datasourceBundle.getDatasource()));
        }
    }

    private FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        return ff;
    }
}
