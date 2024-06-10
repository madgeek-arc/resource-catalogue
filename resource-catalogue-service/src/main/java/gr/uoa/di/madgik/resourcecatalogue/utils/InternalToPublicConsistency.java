package gr.uoa.di.madgik.resourcecatalogue.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
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
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;


    private final PublicProviderManager publicProviderManager;
    private final PublicServiceManager publicServiceManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicInteroperabilityRecordManager publicInteroperabilityRecordManager;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;

    private final SecurityService securityService;
    private final Configuration cfg;
    private final MailService mailService;


    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;
    @Value("${resource.consistency.enable}")
    private boolean enableConsistencyEmails;
    @Value("${resource.consistency.email}")
    private String consistencyEmail;
    @Value("${resource.consistency.cc}")
    private String consistencyCC;

    public InternalToPublicConsistency(ProviderService providerService,
                                       ServiceBundleService serviceBundleService,
                                       TrainingResourceService trainingResourceService,
                                       InteroperabilityRecordService interoperabilityRecordService,
                                       ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                       PublicProviderManager publicProviderManager, PublicServiceManager publicServiceManager,
                                       PublicTrainingResourceManager publicTrainingResourceManager,
                                       PublicInteroperabilityRecordManager publicInteroperabilityRecordManager,
                                       PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager,
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

    @Scheduled(cron = "0 0 0 * * *") // At midnight every day
//    @Scheduled(initialDelay = 0, fixedRate = 6000) // every 2 min
    protected void logInternalToPublicResourceConsistency() {
        List<ProviderBundle> allInternalApprovedProviders = providerService.getAll(createFacetFilter("approved provider"), securityService.getAdminAccess()).getResults();
        List<ServiceBundle> allInternalApprovedServices = serviceBundleService.getAll(createFacetFilter("approved resource"), securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allInternalApprovedTR = trainingResourceService.getAll(createFacetFilter("approved resource"), securityService.getAdminAccess()).getResults();
        List<InteroperabilityRecordBundle> allInternalApprovedIR = interoperabilityRecordService.getAll(createFacetFilter("approved interoperability record"), securityService.getAdminAccess()).getResults();
        List<ResourceInteroperabilityRecordBundle> allInternalApprovedRIR = resourceInteroperabilityRecordService.getAll(createFacetFilter(null), securityService.getAdminAccess()).getResults();
        //TODO: add Configuration Template
        List<String> logs = new ArrayList<>();

        // check consistency for Providers
        for (ProviderBundle providerBundle : allInternalApprovedProviders) {
            String providerId = providerBundle.getId();
            String publicProviderId = providerBundle.getProvider().getCatalogueId() + "." + providerId;
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
            String publicServiceId = serviceBundle.getService().getCatalogueId() + "." + serviceId;
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
            String publicTrainingResourceId = trainingResourceBundle.getTrainingResource().getCatalogueId() + "." + trainingResourceId;
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
            String publicInteroperabilityRecordId = interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId() + "." + interoperabilityRecordId;
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
            String publicResourceInteroperabilityRecordId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId() + "." + resourceInteroperabilityRecordId;
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
                mailService.sendMail(Collections.singletonList(consistencyEmail), null, Collections.singletonList(consistencyCC), subject, teamMail);
            }
            logger.info("\nRecipient: {}\nCC: {}\nTitle: {}\nMail body: \n{}", consistencyEmail, consistencyCC, subject, teamMail);
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
