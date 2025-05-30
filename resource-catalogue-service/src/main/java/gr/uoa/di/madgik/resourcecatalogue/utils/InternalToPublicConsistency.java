/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Component
public class InternalToPublicConsistency {

    private static final Logger logger = LoggerFactory.getLogger(InternalToPublicConsistency.class);

    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;


    private final PublicProviderManager publicProviderManager;
    private final PublicServiceService publicServiceManager;
    private final PublicTrainingResourceService publicTrainingResourceManager;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordManager;
    private final PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager;

    private final SecurityService securityService;
    private final Configuration cfg;
    private final MailService mailService;


    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;
    @Value("${catalogue.email-properties.resource-consistency.enabled:false}")
    private boolean enableConsistencyEmails;
    @Value("${catalogue.email-properties.resource-consistency.to:}")
    private String consistencyTo;
    @Value("${catalogue.email-properties.resource-consistency.cc:}")
    private String consistencyCC;

    public InternalToPublicConsistency(ProviderService providerService,
                                       ServiceBundleService<ServiceBundle> serviceBundleService,
                                       TrainingResourceService trainingResourceService,
                                       InteroperabilityRecordService interoperabilityRecordService,
                                       ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                       PublicProviderManager publicProviderManager, PublicServiceService publicServiceManager,
                                       PublicTrainingResourceService publicTrainingResourceManager,
                                       PublicInteroperabilityRecordService publicInteroperabilityRecordManager,
                                       PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager,
                                       SecurityService securityService, Configuration cfg, MailService mailService) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.publicProviderManager = publicProviderManager;
        this.publicServiceManager = publicServiceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.securityService = securityService;
        this.cfg = cfg;
        this.mailService = mailService;
    }

    //TODO: Add all resource types which get published
    @Scheduled(cron = "0 0 0 * * *") // At midnight every day
//    @Scheduled(initialDelay = 0, fixedRate = 6000) // every 2 min
    protected void logInternalToPublicResourceConsistency() {
        List<ProviderBundle> allInternalApprovedProviders = providerService.getAll(createFacetFilter("approved provider"), securityService.getAdminAccess()).getResults();
        List<ServiceBundle> allInternalApprovedServices = serviceBundleService.getAll(createFacetFilter("approved resource"), securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allInternalApprovedTR = trainingResourceService.getAll(createFacetFilter("approved resource"), securityService.getAdminAccess()).getResults();
        List<InteroperabilityRecordBundle> allInternalApprovedIR = interoperabilityRecordService.getAll(createFacetFilter("approved interoperability record"), securityService.getAdminAccess()).getResults();
        List<ResourceInteroperabilityRecordBundle> allInternalApprovedRIR = resourceInteroperabilityRecordService.getAll(createFacetFilter(null), securityService.getAdminAccess()).getResults();
        List<String> logs = new ArrayList<>();

        // check consistency for Providers
        for (ProviderBundle providerBundle : allInternalApprovedProviders) {
            String providerId = providerBundle.getId();
            String publicProviderId = PublicResourceUtils.createPublicResourceId(providerId, providerBundle.getProvider().getCatalogueId());
            // try and get its Public instance
            try {
                publicProviderManager.get(publicProviderId);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Provider with ID [%s] is missing its Public instance [%s]",
                        providerId, publicProviderId));
            }
        }

        // check consistency for Services
        for (ServiceBundle serviceBundle : allInternalApprovedServices) {
            String serviceId = serviceBundle.getId();
            String publicServiceId = PublicResourceUtils.createPublicResourceId(serviceId, serviceBundle.getService().getCatalogueId());
            // try and get its Public instance
            try {
                publicServiceManager.get(publicServiceId);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Service with ID [%s] is missing its Public instance [%s]",
                        serviceId, publicServiceId));
            }
        }

        // check consistency for Training Resources
        for (TrainingResourceBundle trainingResourceBundle : allInternalApprovedTR) {
            String trainingResourceId = trainingResourceBundle.getId();
            String publicTrainingResourceId = PublicResourceUtils.createPublicResourceId(trainingResourceId,
                    trainingResourceBundle.getTrainingResource().getCatalogueId());
            // try and get its Public instance
            try {
                publicTrainingResourceManager.get(publicTrainingResourceId);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Training Resource with ID [%s] is missing its Public instance [%s]",
                        trainingResourceId, publicTrainingResourceId));
            }
        }

        // check consistency for Interoperability Records
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : allInternalApprovedIR) {
            String interoperabilityRecordId = interoperabilityRecordBundle.getId();
            String publicInteroperabilityRecordId = PublicResourceUtils.createPublicResourceId(interoperabilityRecordId,
                    interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
            // try and get its Public instance
            try {
                publicInteroperabilityRecordManager.get(publicInteroperabilityRecordId);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Interoperability Record with ID [%s] is missing its Public instance [%s]",
                        interoperabilityRecordId, publicInteroperabilityRecordId));
            }
        }

        // check consistency for Resource Interoperability Records
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allInternalApprovedRIR) {
            String resourceInteroperabilityRecordId = resourceInteroperabilityRecordBundle.getId();
            String publicResourceInteroperabilityRecordId = PublicResourceUtils.createPublicResourceId(resourceInteroperabilityRecordId,
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId());
            // try and get its Public instance
            try {
                publicResourceInteroperabilityRecordManager.get(publicResourceInteroperabilityRecordId);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Resource Interoperability Record with ID [%s] is missing its Public instance [%s]",
                        resourceInteroperabilityRecordId, publicResourceInteroperabilityRecordId));
            }
        }

        sendConsistencyEmails(logs);

    }

    protected FacetFilter createFacetFilter(String status) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        if (status != null) {
            ff.addFilter("status", status);
        }
        return ff;
    }

    @Async
    public void sendConsistencyEmails(List<String> logs) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        root.put("logs", logs);
        root.put("project", catalogueName);

        try {
            Template temp = cfg.getTemplate("internalToPublicResourceConsistency.ftl");
            temp.process(root, out);
            String teamMail = out.getBuffer().toString();
            String subject = String.format("[%s Portal] Internal to Public Resource Consistency Logs", catalogueName);
            if (enableConsistencyEmails) {
                mailService.sendMail(Collections.singletonList(consistencyTo), null, Collections.singletonList(consistencyCC), subject, teamMail);
            }
            logger.info("\nRecipient: {}\nCC: {}\nTitle: {}\nMail body: \n{}", consistencyTo, consistencyCC, subject, teamMail);
            out.close();
        } catch (IOException e) {
            logger.error("Error finding mail template", e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }
}
