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

package gr.uoa.di.madgik.resourcecatalogue.utils;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Component
public class InternalToPublicConsistency {

    private static final Logger logger = LoggerFactory.getLogger(InternalToPublicConsistency.class);

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final CatalogueService catalogueService;
    private final TrainingResourceService trainingResourceService;
    private final DeployableApplicationService deployableApplicationService;
    private final AdapterService adapterService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final DatasourceService datasourceService;
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;


    private final PublicOrganisationService publicOrganisationService;
    private final PublicServiceService publicServiceManager;
    private final PublicCatalogueService publicCatalogueService;
    private final PublicTrainingResourceService publicTrainingResourceManager;
    private final PublicDeployableApplicationService publicDeployableApplicationService;
    private final PublicAdapterService publicAdapterService;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordManager;
    private final PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager;
    private final PublicDatasourceService publicDatasourceService;
    private final PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceService;

    private final SecurityService securityService;
    private final Configuration cfg;
    private final MailService mailService;


    @Autowired
    private NodeProperties nodeProperties;
    @Value("${catalogue.homepage}")
    private String projectInstance;
    @Value("${catalogue.email-properties.resource-consistency.enabled:false}")
    private boolean enableConsistencyEmails;
    @Value("${catalogue.email-properties.resource-consistency.to:}")
    private String consistencyTo;
    @Value("${catalogue.email-properties.resource-consistency.cc:}")
    private String consistencyCC;
    @Value("${catalogue.resource-consistency.fix-enabled:false}")
    private boolean enableConsistencyFix;

    public InternalToPublicConsistency(OrganisationService organisationService,
                                       ServiceService serviceService,
                                       CatalogueService catalogueService,
                                       TrainingResourceService trainingResourceService,
                                       InteroperabilityRecordService interoperabilityRecordService,
                                       DeployableApplicationService deployableApplicationService,
                                       AdapterService adapterService,
                                       ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                       DatasourceService datasourceService,
                                       ConfigurationTemplateInstanceService configurationTemplateInstanceService,
                                       PublicOrganisationService publicOrganisationService,
                                       PublicServiceService publicServiceManager,
                                       PublicCatalogueService publicCatalogueService,
                                       PublicTrainingResourceService publicTrainingResourceManager,
                                       PublicDeployableApplicationService publicDeployableApplicationService,
                                       PublicAdapterService publicAdapterService,
                                       PublicInteroperabilityRecordService publicInteroperabilityRecordManager,
                                       PublicDatasourceService publicDatasourceService,
                                       PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceService,
                                       PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager,
                                       SecurityService securityService, Configuration cfg, MailService mailService) {
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.catalogueService = catalogueService;
        this.trainingResourceService = trainingResourceService;
        this.deployableApplicationService = deployableApplicationService;
        this.adapterService = adapterService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.datasourceService = datasourceService;
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.publicOrganisationService = publicOrganisationService;
        this.publicServiceManager = publicServiceManager;
        this.publicCatalogueService = publicCatalogueService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicDeployableApplicationService = publicDeployableApplicationService;
        this.publicAdapterService = publicAdapterService;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.publicDatasourceService = publicDatasourceService;
        this.publicConfigurationTemplateInstanceService = publicConfigurationTemplateInstanceService;
        this.securityService = securityService;
        this.cfg = cfg;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 0 * * *")
//    @Scheduled(initialDelay = 0, fixedRate = 6000)
    protected void logInternalToPublicResourceConsistency() {
        List<OrganisationBundle> allInternalApprovedProviders = organisationService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<ServiceBundle> allInternalApprovedServices = serviceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<CatalogueBundle> allInternalApprovedCatalogues = catalogueService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allInternalApprovedTR = trainingResourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<DeployableApplicationBundle> allInternalApprovedDS = deployableApplicationService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<InteroperabilityRecordBundle> allInternalApprovedIR = interoperabilityRecordService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<ResourceInteroperabilityRecordBundle> allInternalApprovedRIR = resourceInteroperabilityRecordService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<DatasourceBundle> allInternalApprovedDatasources = datasourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<ConfigurationTemplateInstanceBundle> allInternalCTI = configurationTemplateInstanceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<AdapterBundle> allInternalApprovedAdapters = adapterService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<String> logs = new ArrayList<>();

        // check consistency for Providers
        for (OrganisationBundle organisationBundle : allInternalApprovedProviders) {
            // try and get its Public instance
            try {
                publicOrganisationService.get(organisationBundle.getIdentifiers().getPid(),
                        organisationBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Provider with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        organisationBundle.getId(), organisationBundle.getCatalogueId(), organisationBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Adapters
        for (AdapterBundle adapterBundle : allInternalApprovedAdapters) {
            // try and get its Public instance
            try {
                publicAdapterService.get(adapterBundle.getIdentifiers().getPid(),
                        adapterBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Adapter with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        adapterBundle.getId(), adapterBundle.getCatalogueId(),
                        adapterBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Services
        for (ServiceBundle serviceBundle : allInternalApprovedServices) {
            // try and get its Public instance
            try {
                publicServiceManager.get(serviceBundle.getIdentifiers().getPid(),
                        serviceBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Service with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        serviceBundle.getId(), serviceBundle.getCatalogueId(), serviceBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Catalogues
        for (CatalogueBundle catalogueBundle : allInternalApprovedCatalogues) {
            // try and get its Public instance
            try {
                publicCatalogueService.get(catalogueBundle.getIdentifiers().getPid(),
                        catalogueBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Catalogue with ID [%s] is missing its Public instance [%s]",
                        catalogueBundle.getId(), catalogueBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Datasources
        for (DatasourceBundle datasourceBundle : allInternalApprovedDatasources) {
            // try and get its Public instance
            try {
                publicDatasourceService.get(datasourceBundle.getIdentifiers().getPid(),
                        datasourceBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Datasource with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        datasourceBundle.getId(), datasourceBundle.getCatalogueId(),
                        datasourceBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Training Resources
        for (TrainingResourceBundle trainingResourceBundle : allInternalApprovedTR) {
            // try and get its Public instance
            try {
                publicTrainingResourceManager.get(trainingResourceBundle.getIdentifiers().getPid(),
                        trainingResourceBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Training Resource with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        trainingResourceBundle.getId(), trainingResourceBundle.getCatalogueId(),
                        trainingResourceBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Deployable Application
        for (DeployableApplicationBundle deployableApplicationBundle : allInternalApprovedDS) {
            // try and get its Public instance
            try {
                publicDeployableApplicationService.get(deployableApplicationBundle.getIdentifiers().getPid(),
                        deployableApplicationBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Deployable Application with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        deployableApplicationBundle.getId(), deployableApplicationBundle.getCatalogueId(),
                        deployableApplicationBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Interoperability Records
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : allInternalApprovedIR) {
            // try and get its Public instance
            try {
                publicInteroperabilityRecordManager.get(interoperabilityRecordBundle.getIdentifiers().getPid(),
                        interoperabilityRecordBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Interoperability Record with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        interoperabilityRecordBundle.getId(), interoperabilityRecordBundle.getCatalogueId(),
                        interoperabilityRecordBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Resource Interoperability Records
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allInternalApprovedRIR) {
            // try and get its Public instance
            try {
                publicResourceInteroperabilityRecordManager.get(resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
                        resourceInteroperabilityRecordBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Resource Interoperability Record with ID [%s] of the Catalogue [%s] is missing its Public instance [%s]",
                        resourceInteroperabilityRecordBundle.getId(),
                        resourceInteroperabilityRecordBundle.getCatalogueId(),
                        resourceInteroperabilityRecordBundle.getIdentifiers().getPid()));
            }
        }

        // check consistency for Configuration Template Instances
        for (ConfigurationTemplateInstanceBundle ctiBundle : allInternalCTI) {
            // try and get its Public instance
            try {
                publicConfigurationTemplateInstanceService.get(ctiBundle.getIdentifiers().getPid(),
                        ctiBundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logs.add(String.format("Configuration Template Instance with ID [%s] of the internal Catalogue " +
                                "is missing its Public instance [%s]",
                        ctiBundle.getId(), ctiBundle.getIdentifiers().getPid()));
            }
        }

        logger.info("Internal to Public Resource Consistency Logs:\n{}", String.join("\n", logs));
        sendConsistencyEmails(logs);

    }

    @Scheduled(cron = "0 0 1 * * *")
    protected void fixInternalToPublicResourceConsistency() {
        if (!enableConsistencyFix) {
            return;
        }
        logger.info("Starting Internal to Public Resource Consistency Fix...");
        FacetFilter ff = createFixFacetFilter();

        List<OrganisationBundle> providers = organisationService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (OrganisationBundle bundle : providers) {
            try {
                publicOrganisationService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Provider [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicOrganisationService.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Provider [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<AdapterBundle> adapters = adapterService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (AdapterBundle bundle : adapters) {
            try {
                publicAdapterService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Adapter [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicAdapterService.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Adapter [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<ServiceBundle> services = serviceService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (ServiceBundle bundle : services) {
            try {
                publicServiceManager.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Service [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicServiceManager.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Service [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<CatalogueBundle> catalogues = catalogueService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (CatalogueBundle bundle : catalogues) {
            try {
                publicCatalogueService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Catalogue [{}]", bundle.getId());
                try {
                    publicCatalogueService.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Catalogue [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<DatasourceBundle> datasources = datasourceService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (DatasourceBundle bundle : datasources) {
            try {
                publicDatasourceService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Datasource [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicDatasourceService.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Datasource [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<TrainingResourceBundle> trainingResources = trainingResourceService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (TrainingResourceBundle bundle : trainingResources) {
            try {
                publicTrainingResourceManager.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Training Resource [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicTrainingResourceManager.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Training Resource [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<DeployableApplicationBundle> deployableApplications = deployableApplicationService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (DeployableApplicationBundle bundle : deployableApplications) {
            try {
                publicDeployableApplicationService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Deployable Application [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicDeployableApplicationService.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Deployable Application [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (InteroperabilityRecordBundle bundle : interoperabilityRecords) {
            try {
                publicInteroperabilityRecordManager.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Interoperability Record [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicInteroperabilityRecordManager.add(ObjectUtils.clone(bundle), true);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Interoperability Record [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecords =
                resourceInteroperabilityRecordService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (ResourceInteroperabilityRecordBundle bundle : resourceInteroperabilityRecords) {
            try {
                publicResourceInteroperabilityRecordManager.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Resource Interoperability Record [{}] of Catalogue [{}]",
                        bundle.getId(), bundle.getCatalogueId());
                try {
                    publicResourceInteroperabilityRecordManager.add(ObjectUtils.clone(bundle), false);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Resource Interoperability Record [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        List<ConfigurationTemplateInstanceBundle> ctiList = configurationTemplateInstanceService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (ConfigurationTemplateInstanceBundle bundle : ctiList) {
            try {
                publicConfigurationTemplateInstanceService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceNotFoundException e) {
                logger.info("Fixing missing public Configuration Template Instance [{}]", bundle.getId());
                try {
                    publicConfigurationTemplateInstanceService.add(ObjectUtils.clone(bundle), false);
                } catch (Exception ex) {
                    logger.error("Failed to fix public Configuration Template Instance [{}]: {}", bundle.getId(), ex.getMessage(), ex);
                }
            }
        }

        logger.info("Internal to Public Resource Consistency Fix completed.");
    }

    protected FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        ff.addFilter("status", "approved");
        return ff;
    }

    private FacetFilter createFixFacetFilter() {
        FacetFilter ff = createFacetFilter();
        ff.addFilter("active", true);
        return ff;
    }

    @Async
    public void sendConsistencyEmails(List<String> logs) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        root.put("logs", logs);
        root.put("projectInstance", projectInstance);
        root.put("project", nodeProperties.getName());

        try {
            Template temp = cfg.getTemplate("internalToPublicResourceConsistency.ftl");
            temp.process(root, out);
            String teamMail = out.getBuffer().toString();
            String subject = String.format("[%s] Internal to Public Resource Consistency Logs", nodeProperties.getName());
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
