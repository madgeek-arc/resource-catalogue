package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.manager.*;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LogManager.getLogger(ProviderManagementAspect.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final PublicProviderManager publicProviderManager;
    private final PublicServiceManager publicServiceManager;
    private final PublicDatasourceManager publicDatasourceManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicInteroperabilityRecordManager publicInteroperabilityRecordManager;
    private final RegistrationMailService registrationMailService;
    private final SecurityService securityService;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public ProviderManagementAspect(ProviderService<ProviderBundle, Authentication> providerService,
                                    ResourceBundleService<ServiceBundle> serviceBundleService,
                                    ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                    TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                                    PublicProviderManager publicProviderManager,
                                    PublicServiceManager publicServiceManager,
                                    PublicDatasourceManager publicDatasourceManager,
                                    PublicTrainingResourceManager publicTrainingResourceManager,
                                    PublicInteroperabilityRecordManager publicInteroperabilityRecordManager,
                                    PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager,
                                    RegistrationMailService registrationMailService,
                                    SecurityService securityService) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicProviderManager = publicProviderManager;
        this.publicServiceManager = publicServiceManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.registrationMailService = registrationMailService;
        this.securityService = securityService;
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.PendingServiceManager.transformToActive(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.updateResource(..))",
            returning = "serviceBundle")
    public void updateProviderState(ServiceBundle serviceBundle) {
        logger.trace("Updating Provider States");
        updateServiceProviderStates(serviceBundle);
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.PendingDatasourceManager.transformToActive(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.updateResource(..))",
            returning = "datasourceBundle")
    public void updateProviderState(DatasourceBundle datasourceBundle) {
        logger.trace("Updating Provider States");
        updateDatasourceProviderStates(datasourceBundle);
    }

    //TODO: ADD PendingTrainingResourceManager execution
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.updateResource(..))",
            returning = "trainingResourceBundle")
    public void updateProviderState(TrainingResourceBundle trainingResourceBundle) {
        logger.trace("Updating Provider States");
        updateTrainingResourceProviderStates(trainingResourceBundle);
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.PendingProviderManager.transformToActive(..))",
            returning = "providerBundle")
    public void providerRegistrationEmails(ProviderBundle providerBundle) {
        logger.trace("Sending Registration emails");
        if (!providerBundle.getMetadata().isPublished() && providerBundle.getProvider().getCatalogueId().equals(catalogueName)) {
            registrationMailService.sendProviderMails(providerBundle, "providerManager");
        }
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.CatalogueManager.add(..))",
            returning = "catalogueBundle")
    public void catalogueRegistrationEmails(CatalogueBundle catalogueBundle) {
        logger.trace("Sending Registration emails");
        registrationMailService.sendCatalogueMails(catalogueBundle);
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))",
            returning = "serviceBundle")
    public void providerRegistrationEmails(ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "serviceBundleManager");
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.verifyResource(..))",
            returning = "datasourceBundle")
    public void providerRegistrationEmails(DatasourceBundle datasourceBundle) {
        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "datasourceBundleManager");
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))",
            returning = "trainingResourceBundle")
    public void providerRegistrationEmails(TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "trainingResourceManager");
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(..))",
            returning = "providerBundle")
    public void addProviderAsPublic(ProviderBundle providerBundle) {
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            try {
                publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                delayExecution();
                publicProviderManager.add(providerBundle, null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.update(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.auditProvider(..))",
            returning = "providerBundle")
    public void updatePublicProvider(ProviderBundle providerBundle) {
        try {
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            delayExecution();
            publicProviderManager.update(providerBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))",
            returning = "serviceBundle")
    public void updatePublicProviderTemplateStatus(ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation());
        try {
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("Provider with id [%s.%s] is not yet published or does not exist",
                    providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        }
        delayExecution();
        publicProviderManager.update(providerBundle, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.verifyResource(..))",
            returning = "datasourceBundle")
    public void updatePublicProviderTemplateStatus(DatasourceBundle datasourceBundle) {
        ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getResourceOrganisation());
        try {
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("Provider with id [%s.%s] is not yet published or does not exist",
                    providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        }
        delayExecution();
        publicProviderManager.update(providerBundle, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))",
            returning = "trainingResourceBundle")
    public void updatePublicProviderTemplateStatus(TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        try {
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("Provider with id [%s.%s] is not yet published or does not exist",
                    providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        }
        delayExecution();
        publicProviderManager.update(providerBundle, null);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ProviderManager.delete(..))")
    public void deletePublicProvider(JoinPoint joinPoint) {
        ProviderBundle providerBundle = (ProviderBundle) joinPoint.getArgs()[0];
        publicProviderManager.delete(providerBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.PendingServiceManager.transformToActive(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.changeProvider(..))",
            returning = "serviceBundle")
    public void addResourceAsPublic(ServiceBundle serviceBundle) {
        if (serviceBundle.getStatus().equals("approved resource") && serviceBundle.isActive()) {
            try {
                publicServiceManager.get(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                delayExecution();
                publicServiceManager.add(serviceBundle, null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.PendingDatasourceManager.transformToActive(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.changeProvider(..))",
            returning = "datasourceBundle")
    public void addResourceAsPublic(DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getStatus().equals("approved resource") && datasourceBundle.isActive()) {
            try {
                publicDatasourceManager.get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                delayExecution();
                publicDatasourceManager.add(datasourceBundle, null);
            }
        }
    }

    //TODO: ADD PendingTrainingResourceManager execution
    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.changeProvider(..))",
            returning = "trainingResourceBundle")
    public void addResourceAsPublic(TrainingResourceBundle trainingResourceBundle) {
        if (trainingResourceBundle.getStatus().equals("approved resource") && trainingResourceBundle.isActive()) {
            try {
                publicTrainingResourceManager.get(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                delayExecution();
                publicTrainingResourceManager.add(trainingResourceBundle, null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.verifyResource(..))",
            returning = "interoperabilityRecordBundle")
    public void addResourceAsPublic(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        if (interoperabilityRecordBundle.getStatus().equals("approved interoperability record") && interoperabilityRecordBundle.isActive()){
            try{
                publicInteroperabilityRecordManager.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e){
                delayExecution();
                publicInteroperabilityRecordManager.add(interoperabilityRecordBundle, null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.updateResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.auditResource(..))",
            returning = "serviceBundle")
    public void updatePublicResource(ServiceBundle serviceBundle) {
        try {
            publicServiceManager.get(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
            delayExecution();
            publicServiceManager.update(serviceBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.updateResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.auditResource(..))",
            returning = "datasourceBundle")
    public void updatePublicResource(DatasourceBundle datasourceBundle) {
        try {
            publicDatasourceManager.get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
            delayExecution();
            publicDatasourceManager.update(datasourceBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.updateResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.auditResource(..))",
            returning = "trainingResourceBundle")
    public void updatePublicResource(TrainingResourceBundle trainingResourceBundle) {
        try {
            publicTrainingResourceManager.get(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
            delayExecution();
            publicTrainingResourceManager.update(trainingResourceBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.update(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.verifyResource(..))",
            returning = "interoperabilityRecordBundle")
    public void updatePublicResource(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try{
            publicInteroperabilityRecordManager.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
            delayExecution();
            publicInteroperabilityRecordManager.update(interoperabilityRecordBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ServiceBundleManager.delete(..))")
    public void deletePublicService(JoinPoint joinPoint) {
        ServiceBundle serviceBundle = (ServiceBundle) joinPoint.getArgs()[0];
        publicServiceManager.delete(serviceBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.DatasourceBundleManager.delete(..))")
    public void deletePublicDatasource(JoinPoint joinPoint) {
        DatasourceBundle datasourceBundle = (DatasourceBundle) joinPoint.getArgs()[0];
        publicDatasourceManager.delete(datasourceBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.TrainingResourceManager.delete(..))")
    public void deletePublicTrainingResource(JoinPoint joinPoint) {
        TrainingResourceBundle trainingResourceBundle = (TrainingResourceBundle) joinPoint.getArgs()[0];
        publicTrainingResourceManager.delete(trainingResourceBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.delete(..))")
    public void deletePublicInteroperabilityRecord(JoinPoint joinPoint) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = (InteroperabilityRecordBundle) joinPoint.getArgs()[0];
        publicInteroperabilityRecordManager.delete(interoperabilityRecordBundle);
    }

    //TODO: Probably no needed

    /**
     * This method is used to update a list of new providers with status
     * 'Provider.States.ST_SUBMISSION' or 'Provider.States.REJECTED_ST'
     * to status 'Provider.States.PENDING_2'
     *
     * @param serviceBundle
     */
    @Async
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void updateServiceProviderStates(ServiceBundle serviceBundle) {
        if (serviceBundle.getService().getCatalogueId().equals(catalogueName)) {
            try {
                ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(), null);
                if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
                    logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                            serviceBundle.getService().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
                    serviceBundleService.verifyResource(serviceBundle.getService().getId(), "pending resource", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e);
            }
        }
    }

    @Async
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void updateDatasourceProviderStates(DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueName)) {
            try {
                ProviderBundle providerBundle = providerService.get(datasourceBundle.getDatasource().getResourceOrganisation(), null);
                if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
                    logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                            datasourceBundle.getDatasource().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
                    datasourceBundleService.verifyResource(datasourceBundle.getDatasource().getId(), "pending resource", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e);
            }
        }
    }

    @Async
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void updateTrainingResourceProviderStates(TrainingResourceBundle trainingResourceBundle) {
        if (trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueName)) {
            try {
                ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(), null);
                if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
                    logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                            trainingResourceBundle.getTrainingResource().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
                    trainingResourceService.verifyResource(trainingResourceBundle.getTrainingResource().getId(), "pending resource", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ResourceInteroperabilityRecordManager.add(..))",
            returning = "interoperabilityBundle")
    public void addResourceInteroperabilityRecordAsPublic(ResourceInteroperabilityRecordBundle interoperabilityBundle) {
        // TODO: check Resource states (publish if only approved/active)
        try {
            publicResourceInteroperabilityManager.get(String.format("%s.%s",
                    interoperabilityBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                    interoperabilityBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            delayExecution();
            publicResourceInteroperabilityManager.add(interoperabilityBundle, null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ResourceInteroperabilityRecordManager.update(..))",
            returning = "interoperabilityBundle")
    public void updatePublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle interoperabilityBundle) {
        try {
            publicResourceInteroperabilityManager.get(String.format("%s.%s", interoperabilityBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                    interoperabilityBundle.getId()));
            delayExecution();
            publicResourceInteroperabilityManager.update(interoperabilityBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ResourceInteroperabilityRecordManager.delete(..))")
    public void deletePublicResourceInteroperabilityRecord(JoinPoint joinPoint) {
        ResourceInteroperabilityRecordBundle interoperabilityBundle = (ResourceInteroperabilityRecordBundle) joinPoint.getArgs()[0];
        publicResourceInteroperabilityManager.delete(interoperabilityBundle);
    }

    private void delayExecution() {
        try {
            Thread.sleep(20_000); // 20sec
        } catch (InterruptedException ex) {
            logger.error(ex);
        }
    }
}
