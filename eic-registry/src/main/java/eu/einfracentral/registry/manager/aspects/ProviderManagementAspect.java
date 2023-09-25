package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.manager.*;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ObjectUtils;
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
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final PublicProviderManager publicProviderManager;
    private final PublicServiceManager publicServiceManager;
    private final PublicDatasourceManager publicDatasourceManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicInteroperabilityRecordManager publicInteroperabilityRecordManager;
    private final PublicConfigurationTemplateImplementationManager publicConfigurationTemplateImplementationManager;
    private final RegistrationMailService registrationMailService;
    private final SecurityService securityService;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public ProviderManagementAspect(ProviderService<ProviderBundle, Authentication> providerService,
                                    ServiceBundleService<ServiceBundle> serviceBundleService,
                                    TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                                    PublicProviderManager publicProviderManager,
                                    PublicServiceManager publicServiceManager,
                                    PublicDatasourceManager publicDatasourceManager,
                                    PublicTrainingResourceManager publicTrainingResourceManager,
                                    PublicInteroperabilityRecordManager publicInteroperabilityRecordManager,
                                    PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager,
                                    PublicConfigurationTemplateImplementationManager publicConfigurationTemplateImplementationManager,
                                    RegistrationMailService registrationMailService,
                                    SecurityService securityService) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicProviderManager = publicProviderManager;
        this.publicServiceManager = publicServiceManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.publicConfigurationTemplateImplementationManager = publicConfigurationTemplateImplementationManager;
        this.registrationMailService = registrationMailService;
        this.securityService = securityService;
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.PendingServiceManager.transformToActive(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.updateResource(..))",
            returning = "serviceBundle")
    public void updateProviderState(final ServiceBundle serviceBundle) {
        logger.trace("Updating Provider States");
        updateServiceProviderStates(serviceBundle);
    }

    //TODO: ADD PendingTrainingResourceManager execution
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.updateResource(..))",
            returning = "trainingResourceBundle")
    public void updateProviderState(final TrainingResourceBundle trainingResourceBundle) {
        logger.trace("Updating Provider States");
        updateTrainingResourceProviderStates(trainingResourceBundle);
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.PendingProviderManager.transformToActive(..))",
            returning = "providerBundle")
    public void providerRegistrationEmails(final ProviderBundle providerBundle) {
        logger.trace("Sending Registration emails");
        if (!providerBundle.getMetadata().isPublished() && providerBundle.getProvider().getCatalogueId().equals(catalogueName)) {
            registrationMailService.sendProviderMails(providerBundle, "providerManager");
        }
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.CatalogueManager.add(..))",
            returning = "catalogueBundle")
    public void catalogueRegistrationEmails(final CatalogueBundle catalogueBundle) {
        logger.trace("Sending Registration emails");
        registrationMailService.sendCatalogueMails(catalogueBundle);
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))",
            returning = "serviceBundle")
    public void providerRegistrationEmails(final ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "serviceBundleManager");
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))",
            returning = "trainingResourceBundle")
    public void providerRegistrationEmails(final TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "trainingResourceManager");
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(..))",
            returning = "providerBundle")
    public void addProviderAsPublic(final ProviderBundle providerBundle) {
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            try {
                publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicProviderManager.add(ObjectUtils.clone(providerBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.update(..)) " +
            "&& args(providerBundle,..)", returning = "ret", argNames = "providerBundle,ret")
    public void updatePublicProvider(ProviderBundle providerBundle, ProviderBundle ret) {
        try {
            if (!ret.equals(providerBundle)) {
                publicProviderManager.update(ObjectUtils.clone(providerBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.suspend(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.auditProvider(..))",
            returning = "providerBundle")
    public void updatePublicProvider(final ProviderBundle providerBundle) {
        try {
            publicProviderManager.update(ObjectUtils.clone(providerBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))",
            returning = "serviceBundle")
    public void updatePublicProviderTemplateStatus(final ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation());
        checkIfPublicProviderExistsOrElseThrow(providerBundle);
        publicProviderManager.update(providerBundle, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))",
            returning = "trainingResourceBundle")
    public void updatePublicProviderTemplateStatus(final TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        checkIfPublicProviderExistsOrElseThrow(providerBundle);
        publicProviderManager.update(providerBundle, null);
    }

    private void checkIfPublicProviderExistsOrElseThrow(ProviderBundle providerBundle) {
        try {
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("Provider with id [%s.%s] is not yet published or does not exist",
                    providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        }
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
    public void addResourceAsPublic(final ServiceBundle serviceBundle) {
        if (serviceBundle.getStatus().equals("approved resource") && serviceBundle.isActive()) {
            try {
                publicServiceManager.get(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicServiceManager.add(ObjectUtils.clone(serviceBundle), null);
            }
        }
    }

    //TODO: ADD PendingTrainingResourceManager execution
    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.addResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.changeProvider(..))",
            returning = "trainingResourceBundle")
    public void addResourceAsPublic(final TrainingResourceBundle trainingResourceBundle) {
        if (trainingResourceBundle.getStatus().equals("approved resource") && trainingResourceBundle.isActive()) {
            try {
                publicTrainingResourceManager.get(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicTrainingResourceManager.add(ObjectUtils.clone(trainingResourceBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.verifyResource(..))",
            returning = "interoperabilityRecordBundle")
    public void addResourceAsPublic(final InteroperabilityRecordBundle interoperabilityRecordBundle) {
        if (interoperabilityRecordBundle.getStatus().equals("approved interoperability record") && interoperabilityRecordBundle.isActive()){
            try{
                publicInteroperabilityRecordManager.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e){
                publicInteroperabilityRecordManager.add(ObjectUtils.clone(interoperabilityRecordBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.updateResource(..)) " +
            "&& args(serviceBundle,..)", returning = "ret", argNames = "serviceBundle,ret")
    public void updatePublicResource(ServiceBundle serviceBundle, ServiceBundle ret) {
        try {
            if (!ret.equals(serviceBundle)) {
                publicServiceManager.update(ObjectUtils.clone(serviceBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ServiceBundleManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.suspend(..))" +
            "|| execution(* eu.einfracentral.registry.manager.ServiceBundleManager.auditResource(..))",
            returning = "serviceBundle")
    public void updatePublicResource(final ServiceBundle serviceBundle) {
        try {
            publicServiceManager.update(ObjectUtils.clone(serviceBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.updateResource(..)) " +
            "&& args(trainingResourceBundle,..)", returning = "ret", argNames = "trainingResourceBundle,ret")
    public void updatePublicResource(TrainingResourceBundle trainingResourceBundle, TrainingResourceBundle ret) {
        try {
            if (!ret.equals(trainingResourceBundle)) {
                publicTrainingResourceManager.update(ObjectUtils.clone(trainingResourceBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.TrainingResourceManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.suspend(..))" +
            "|| execution(* eu.einfracentral.registry.manager.TrainingResourceManager.auditResource(..))",
            returning = "trainingResourceBundle")
    public void updatePublicResource(final TrainingResourceBundle trainingResourceBundle) {
        try {
            publicTrainingResourceManager.update(ObjectUtils.clone(trainingResourceBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.update(..)) " +
            "&& args(interoperabilityRecordBundle,..)", returning = "ret", argNames = "interoperabilityRecordBundle,ret")
    public void updatePublicResource(InteroperabilityRecordBundle interoperabilityRecordBundle, InteroperabilityRecordBundle ret) {
        try {
            if (!ret.equals(interoperabilityRecordBundle)) {
                publicInteroperabilityRecordManager.update(ObjectUtils.clone(interoperabilityRecordBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.publish(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.verifyResource(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.suspend(..))" +
            "|| execution(* eu.einfracentral.registry.manager.InteroperabilityRecordManager.auditResource(..))",
            returning = "interoperabilityRecordBundle")
    public void updatePublicResource(final InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try{
            publicInteroperabilityRecordManager.update(ObjectUtils.clone(interoperabilityRecordBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore){}
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ServiceBundleManager.delete(..))")
    public void deletePublicService(JoinPoint joinPoint) {
        ServiceBundle serviceBundle = (ServiceBundle) joinPoint.getArgs()[0];
        publicServiceManager.delete(serviceBundle);
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
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceManager.add(..))" +
            "|| execution(* eu.einfracentral.registry.manager.DatasourceManager.verifyDatasource(..))",
            returning = "datasourceBundle")
    public void addDatasourceAsPublic(final DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getStatus().equals("approved datasource") && datasourceBundle.isActive()) {
            try {
                publicDatasourceManager.get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicDatasourceManager.add(ObjectUtils.clone(datasourceBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceManager.update(..)) " +
            "&& args(datasourceBundle,..)", returning = "ret", argNames = "datasourceBundle,ret")
    public void updatePublicResource(DatasourceBundle datasourceBundle, DatasourceBundle ret) {
        try {
            if (!ret.equals(datasourceBundle)) {
                publicDatasourceManager.update(ObjectUtils.clone(datasourceBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.DatasourceManager.verifyDatasource(..))",
            returning = "datasourceBundle")
    public void updatePublicDatasource(final DatasourceBundle datasourceBundle) {
        try {
            publicDatasourceManager.update(ObjectUtils.clone(datasourceBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.DatasourceManager.delete(..))")
    public void deletePublicDatasource(JoinPoint joinPoint) {
        DatasourceBundle datasourceBundle = (DatasourceBundle) joinPoint.getArgs()[0];
        publicDatasourceManager.delete(datasourceBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ResourceInteroperabilityRecordManager.add(..))",
            returning = "resourceInteroperabilityRecordBundle")
    public void addResourceInteroperabilityRecordAsPublic(final ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // TODO: check Resource states (publish if only approved/active)
        try {
            publicResourceInteroperabilityRecordManager.get(String.format("%s.%s",
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                    resourceInteroperabilityRecordBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            publicResourceInteroperabilityRecordManager.add(ObjectUtils.clone(resourceInteroperabilityRecordBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ResourceInteroperabilityRecordManager.update(..)) " +
            "&& args(resourceInteroperabilityRecordBundle,..)", returning = "ret", argNames = "resourceInteroperabilityRecordBundle,ret")
    public void updatePublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, ResourceInteroperabilityRecordBundle ret) {
        try {
            if (!ret.equals(resourceInteroperabilityRecordBundle)) {
                publicResourceInteroperabilityRecordManager.update(ObjectUtils.clone(resourceInteroperabilityRecordBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ResourceInteroperabilityRecordManager.delete(..))")
    public void deletePublicResourceInteroperabilityRecord(JoinPoint joinPoint) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = (ResourceInteroperabilityRecordBundle) joinPoint.getArgs()[0];
        publicResourceInteroperabilityRecordManager.delete(resourceInteroperabilityRecordBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ConfigurationTemplateInstanceManager.add(..))",
            returning = "configurationTemplateInstanceBundle")
    public void addConfigurationTemplateInstanceAsPublic(final ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        try{
            publicConfigurationTemplateImplementationManager.get(String.format("%s.%s", catalogueName, configurationTemplateInstanceBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e){
            publicConfigurationTemplateImplementationManager.add(ObjectUtils.clone(configurationTemplateInstanceBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ConfigurationTemplateInstanceManager.update(..)) " +
            "&& args(configurationTemplateInstanceBundle,..)", returning = "ret", argNames = "configurationTemplateInstanceBundle,ret")
    public void updatePublicConfigurationTemplateInstance(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, ConfigurationTemplateInstanceBundle ret) {
        try {
            if (!ret.equals(configurationTemplateInstanceBundle)) {
                publicConfigurationTemplateImplementationManager.update(ObjectUtils.clone(configurationTemplateInstanceBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ConfigurationTemplateInstanceManager.delete(..))")
    public void deletePublicConfigurationTemplateInstance(JoinPoint joinPoint) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = (ConfigurationTemplateInstanceBundle) joinPoint.getArgs()[0];
        publicConfigurationTemplateImplementationManager.delete(configurationTemplateInstanceBundle);
    }
}
