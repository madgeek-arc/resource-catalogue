/**
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
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
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
    private final DatasourceService datasourceService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;


    private final PublicProviderService publicProviderService;
    private final PublicServiceService publicServiceManager;
    private final PublicTrainingResourceService publicTrainingResourceManager;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordManager;
    private final PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager;
    private final PublicDatasourceService publicDatasourceService;
    private final PublicHelpdeskService publicHelpdeskService;
    private final PublicMonitoringService publicMonitoringService;
    private final PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceService;

    private final SecurityService securityService;
    private final Configuration cfg;
    private final MailService mailService;


    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;
    @Value("${catalogue.homepage}")
    private String projectInstance;
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
                                       DatasourceService datasourceService, HelpdeskService helpdeskService,
                                       MonitoringService monitoringService,
                                       ConfigurationTemplateInstanceService configurationTemplateInstanceService,
                                       PublicProviderService publicProviderService, PublicServiceService publicServiceManager,
                                       PublicTrainingResourceService publicTrainingResourceManager,
                                       PublicInteroperabilityRecordService publicInteroperabilityRecordManager,
                                       PublicDatasourceService publicDatasourceService, PublicHelpdeskService publicHelpdeskService,
                                       PublicMonitoringService publicMonitoringService,
                                       PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceService,
                                       PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager,
                                       SecurityService securityService, Configuration cfg, MailService mailService) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.datasourceService = datasourceService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.publicProviderService = publicProviderService;
        this.publicServiceManager = publicServiceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.publicDatasourceService = publicDatasourceService;
        this.publicHelpdeskService = publicHelpdeskService;
        this.publicMonitoringService = publicMonitoringService;
        this.publicConfigurationTemplateInstanceService = publicConfigurationTemplateInstanceService;
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
        List<DatasourceBundle> allInternalApprovedDatasources = datasourceService.getAll(createFacetFilter(null), securityService.getAdminAccess()).getResults();
        List<HelpdeskBundle> allInternalHelpdesks = helpdeskService.getAll(createFacetFilter(null), securityService.getAdminAccess()).getResults();
        List<MonitoringBundle> allInternalMonitorings = monitoringService.getAll(createFacetFilter(null), securityService.getAdminAccess()).getResults();
        List<ConfigurationTemplateInstanceBundle> allInternalCTI = configurationTemplateInstanceService.getAll(createFacetFilter(null), securityService.getAdminAccess()).getResults();
        List<String> logs = new ArrayList<>();

        // check consistency for Providers
        for (ProviderBundle providerBundle : allInternalApprovedProviders) {
            // try and get its Public instance
            try {
                publicProviderService.get(providerBundle.getIdentifiers().getPid(),
                        providerBundle.getProvider().getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Provider with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        providerBundle.getId(), providerBundle.getProvider().getCatalogueId(), providerBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Services
        for (ServiceBundle serviceBundle : allInternalApprovedServices) {
            // try and get its Public instance
            try {
                publicServiceManager.get(serviceBundle.getIdentifiers().getPid(),
                        serviceBundle.getService().getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Service with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        serviceBundle.getId(), serviceBundle.getService().getCatalogueId(), serviceBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Training Resources
        for (TrainingResourceBundle trainingResourceBundle : allInternalApprovedTR) {
            // try and get its Public instance
            try {
                publicTrainingResourceManager.get(trainingResourceBundle.getIdentifiers().getPid(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Training Resource with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        trainingResourceBundle.getId(), trainingResourceBundle.getTrainingResource().getCatalogueId(),
                        trainingResourceBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Interoperability Records
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : allInternalApprovedIR) {
            // try and get its Public instance
            try {
                publicInteroperabilityRecordManager.get(interoperabilityRecordBundle.getIdentifiers().getPid(),
                        interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Interoperability Record with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        interoperabilityRecordBundle.getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                        interoperabilityRecordBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Resource Interoperability Records
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allInternalApprovedRIR) {
            // try and get its Public instance
            try {
                publicResourceInteroperabilityRecordManager.get(resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Resource Interoperability Record with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        resourceInteroperabilityRecordBundle.getId(),
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                        resourceInteroperabilityRecordBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Datasources
        for (DatasourceBundle datasourceBundle : allInternalApprovedDatasources) {
            // try and get its Public instance
            try {
                publicDatasourceService.get(datasourceBundle.getIdentifiers().getPid(),
                        datasourceBundle.getDatasource().getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Datasource with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        datasourceBundle.getId(), datasourceBundle.getDatasource().getCatalogueId(),
                        datasourceBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Helpdesks
        for (HelpdeskBundle helpdeskBundle : allInternalHelpdesks) {
            // try and get its Public instance
            try {
                publicHelpdeskService.get(helpdeskBundle.getIdentifiers().getPid(),
                        helpdeskBundle.getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Helpdesk with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        helpdeskBundle.getId(), helpdeskBundle.getCatalogueId(), helpdeskBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Monitorings
        for (MonitoringBundle monitoringBundle : allInternalMonitorings) {
            // try and get its Public instance
            try {
                publicMonitoringService.get(monitoringBundle.getIdentifiers().getPid(),
                        monitoringBundle.getCatalogueId(), true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Monitoring with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        monitoringBundle.getId(), monitoringBundle.getCatalogueId(), monitoringBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Configuration Template Instances
        for (ConfigurationTemplateInstanceBundle ctiBundle : allInternalCTI) {
            // try and get its Public instance
            try {
                publicConfigurationTemplateInstanceService.get(ctiBundle.getIdentifiers().getPid(),
                        null, true);
            } catch (ResourceException | ResourceNotFoundException e) {
                logs.add(String.format("Configuration Template Instance with ID [%s] of the internal Catalogue is missing its Public instance [%s]",
                        ctiBundle.getId(), ctiBundle.getIdentifiers().getPid()));
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
        root.put("projectInstance", projectInstance);
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
