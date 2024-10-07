package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Profile("beyond")
@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManagementAspect.class);

    private final ProviderService providerService;
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final PublicProviderManager publicProviderManager;
    private final PublicServiceManager publicServiceManager;
    private final PublicDatasourceManager publicDatasourceManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicInteroperabilityRecordManager publicInteroperabilityRecordManager;
    private final PublicConfigurationTemplateInstanceManager publicConfigurationTemplateInstanceManager;
    private final RegistrationMailService registrationMailService;
    private final SecurityService securityService;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;
    private final PublicResourceUtils publicResourceUtils;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ProviderManagementAspect(ProviderService providerService,
                                    ServiceBundleService serviceBundleService,
                                    TrainingResourceService trainingResourceService,
                                    PublicProviderManager publicProviderManager,
                                    PublicServiceManager publicServiceManager,
                                    PublicDatasourceManager publicDatasourceManager,
                                    PublicTrainingResourceManager publicTrainingResourceManager,
                                    PublicInteroperabilityRecordManager publicInteroperabilityRecordManager,
                                    PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager,
                                    PublicConfigurationTemplateInstanceManager publicConfigurationTemplateInstanceManager,
                                    RegistrationMailService registrationMailService,
                                    SecurityService securityService,
                                    PublicResourceUtils publicResourceUtils) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicProviderManager = publicProviderManager;
        this.publicServiceManager = publicServiceManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.publicConfigurationTemplateInstanceManager = publicConfigurationTemplateInstanceManager;
        this.registrationMailService = registrationMailService;
        this.securityService = securityService;
        this.publicResourceUtils = publicResourceUtils;
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftServiceManager.transformToNonDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.addResource(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.updateResource(..))",
            returning = "serviceBundle")
    public void updateProviderState(final ServiceBundle serviceBundle) {
        logger.trace("Updating Provider States");
        updateServiceProviderStates(serviceBundle);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftTrainingResourceManager.transformToNonDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..))",
            returning = "trainingResourceBundle")
    public void updateProviderState(final TrainingResourceBundle trainingResourceBundle) {
        logger.trace("Updating Provider States");
        updateTrainingResourceProviderStates(trainingResourceBundle);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftProviderManager.transformToNonDraft(..))",
            returning = "providerBundle")
    public void providerRegistrationEmails(final ProviderBundle providerBundle) {
        logger.trace("Sending Registration emails");
        if (!providerBundle.getMetadata().isPublished() && providerBundle.getProvider().getCatalogueId().equals(catalogueId)) {
            registrationMailService.sendProviderMails(providerBundle, "providerManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.add(..))",
            returning = "catalogueBundle")
    public void catalogueRegistrationEmails(final CatalogueBundle catalogueBundle) {
        logger.trace("Sending Registration emails");
        registrationMailService.sendCatalogueMails(catalogueBundle);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))",
            returning = "serviceBundle")
    public void providerRegistrationEmails(final ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "serviceBundleManager");
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "trainingResourceBundle")
    public void providerRegistrationEmails(final TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle, "trainingResourceManager");
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))",
            returning = "providerBundle")
    public void addProviderAsPublic(final ProviderBundle providerBundle) {
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            try {
                publicProviderManager.get(publicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                        providerBundle.getProvider().getCatalogueId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicProviderManager.add(ObjectUtils.clone(providerBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.update(..)) " +
            "&& args(providerBundle,..)", returning = "ret", argNames = "providerBundle,ret")
    public void updatePublicProvider(ProviderBundle providerBundle, ProviderBundle ret) {
        try {
            if (!ret.equals(providerBundle)) {
                publicProviderManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.publish(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.suspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.audit(..))",
            returning = "providerBundle")
    public void updatePublicProvider(final ProviderBundle providerBundle) {
        try {
            publicProviderManager.update(ObjectUtils.clone(providerBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))",
            returning = "serviceBundle")
    public void updatePublicProviderTemplateStatus(final ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation());
        checkIfPublicProviderExistsOrElseThrow(providerBundle);
        publicProviderManager.update(providerBundle, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "trainingResourceBundle")
    public void updatePublicProviderTemplateStatus(final TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        checkIfPublicProviderExistsOrElseThrow(providerBundle);
        publicProviderManager.update(providerBundle, null);
    }

    private void checkIfPublicProviderExistsOrElseThrow(ProviderBundle providerBundle) {
        String publicId = publicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId());
        try {
            publicProviderManager.get(publicId);
        } catch (ResourceException | ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("Provider with id [%s] is not yet published or does not exist",
                    publicId));
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.delete(..))")
    public void deletePublicProvider(JoinPoint joinPoint) {
        ProviderBundle providerBundle = (ProviderBundle) joinPoint.getArgs()[0];
        publicProviderManager.delete(providerBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.addResource(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftServiceManager.transformToNonDraft(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.changeProvider(..))",
            returning = "serviceBundle")
    public void addResourceAsPublic(final ServiceBundle serviceBundle) {
        if (serviceBundle.getStatus().equals("approved resource") && serviceBundle.isActive()) {
            try {
                publicServiceManager.get(publicResourceUtils.createPublicResourceId(serviceBundle.getService().getId(),
                        serviceBundle.getService().getCatalogueId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicServiceManager.add(ObjectUtils.clone(serviceBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftTrainingResourceManager.transformToNonDraft(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.changeProvider(..))",
            returning = "trainingResourceBundle")
    public void addResourceAsPublic(final TrainingResourceBundle trainingResourceBundle) {
        if (trainingResourceBundle.getStatus().equals("approved resource") && trainingResourceBundle.isActive()) {
            try {
                publicTrainingResourceManager.get(publicResourceUtils.createPublicResourceId(trainingResourceBundle.getTrainingResource().getId(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicTrainingResourceManager.add(ObjectUtils.clone(trainingResourceBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))",
            returning = "interoperabilityRecordBundle")
    public void addResourceAsPublic(final InteroperabilityRecordBundle interoperabilityRecordBundle) {
        if (interoperabilityRecordBundle.getStatus().equals("approved interoperability record") && interoperabilityRecordBundle.isActive()) {
            try {
                publicInteroperabilityRecordManager.get(publicResourceUtils.createPublicResourceId(interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                        interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicInteroperabilityRecordManager.add(ObjectUtils.clone(interoperabilityRecordBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.updateResource(..)) " +
            "&& args(serviceBundle,..)", returning = "ret", argNames = "serviceBundle,ret")
    public void updatePublicResource(ServiceBundle serviceBundle, ServiceBundle ret) {
        try {
            if (!ret.equals(serviceBundle)) {
                publicServiceManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.publish(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.suspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.audit(..))",
            returning = "serviceBundle")
    public void updatePublicResource(final ServiceBundle serviceBundle) {
        try {
            publicServiceManager.update(ObjectUtils.clone(serviceBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..)) " +
            "&& args(trainingResourceBundle,..)", returning = "ret", argNames = "trainingResourceBundle,ret")
    public void updatePublicResource(TrainingResourceBundle trainingResourceBundle, TrainingResourceBundle ret) {
        try {
            if (!ret.equals(trainingResourceBundle)) {
                publicTrainingResourceManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.publish(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.suspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.audit(..))",
            returning = "trainingResourceBundle")
    public void updatePublicResource(final TrainingResourceBundle trainingResourceBundle) {
        try {
            publicTrainingResourceManager.update(ObjectUtils.clone(trainingResourceBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.update(..)) " +
            "&& args(interoperabilityRecordBundle,..)", returning = "ret", argNames = "interoperabilityRecordBundle,ret")
    public void updatePublicResource(InteroperabilityRecordBundle interoperabilityRecordBundle, InteroperabilityRecordBundle ret) {
        try {
            if (!ret.equals(interoperabilityRecordBundle)) {
                publicInteroperabilityRecordManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.publish(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.suspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.audit(..))",
            returning = "interoperabilityRecordBundle")
    public void updatePublicResource(final InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try {
            publicInteroperabilityRecordManager.update(ObjectUtils.clone(interoperabilityRecordBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.delete(..))")
    public void deletePublicService(JoinPoint joinPoint) {
        ServiceBundle serviceBundle = (ServiceBundle) joinPoint.getArgs()[0];
        publicServiceManager.delete(serviceBundle);
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.delete(..))")
    public void deletePublicTrainingResource(JoinPoint joinPoint) {
        TrainingResourceBundle trainingResourceBundle = (TrainingResourceBundle) joinPoint.getArgs()[0];
        publicTrainingResourceManager.delete(trainingResourceBundle);
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.delete(..))")
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
    public void updateServiceProviderStates(ServiceBundle serviceBundle) {
        if (serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(), null);
                if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
                    logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                            serviceBundle.getService().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
                    serviceBundleService.verify(serviceBundle.getService().getId(), "pending resource", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Async
    public void updateTrainingResourceProviderStates(TrainingResourceBundle trainingResourceBundle) {
        if (trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(), null);
                if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
                    logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                            trainingResourceBundle.getTrainingResource().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
                    trainingResourceService.verify(trainingResourceBundle.getTrainingResource().getId(), "pending resource", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasourceBundle")
    public void addDatasourceAsPublic(final DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getStatus().equals("approved datasource") && datasourceBundle.isActive()) {
            try {
                publicDatasourceManager.get(publicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                        datasourceBundle.getDatasource().getCatalogueId()));
            } catch (ResourceException | ResourceNotFoundException e) {
                publicDatasourceManager.add(ObjectUtils.clone(datasourceBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.update(..)) " +
            "&& args(datasourceBundle,..)", returning = "ret", argNames = "datasourceBundle,ret")
    public void updatePublicResource(DatasourceBundle datasourceBundle, DatasourceBundle ret) {
        try {
            if (!ret.equals(datasourceBundle)) {
                publicDatasourceManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasourceBundle")
    public void updatePublicDatasource(final DatasourceBundle datasourceBundle) {
        try {
            publicDatasourceManager.update(ObjectUtils.clone(datasourceBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.delete(..))")
    public void deletePublicDatasource(JoinPoint joinPoint) {
        DatasourceBundle datasourceBundle = (DatasourceBundle) joinPoint.getArgs()[0];
        publicDatasourceManager.delete(datasourceBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.add(..))",
            returning = "resourceInteroperabilityRecordBundle")
    public void addResourceInteroperabilityRecordAsPublic(final ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // TODO: check Resource states (publish if only approved/active)
        try {
            publicResourceInteroperabilityRecordManager.get(publicResourceUtils.createPublicResourceId(
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getId(),
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            publicResourceInteroperabilityRecordManager.add(ObjectUtils.clone(resourceInteroperabilityRecordBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.update(..)) " +
            "&& args(resourceInteroperabilityRecordBundle,..)", returning = "ret", argNames = "resourceInteroperabilityRecordBundle,ret")
    public void updatePublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, ResourceInteroperabilityRecordBundle ret) {
        try {
            if (!ret.equals(resourceInteroperabilityRecordBundle)) {
                publicResourceInteroperabilityRecordManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.delete(..))")
    public void deletePublicResourceInteroperabilityRecord(JoinPoint joinPoint) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = (ResourceInteroperabilityRecordBundle) joinPoint.getArgs()[0];
        publicResourceInteroperabilityRecordManager.delete(resourceInteroperabilityRecordBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.add(..))",
            returning = "configurationTemplateInstanceBundle")
    public void addConfigurationTemplateInstanceAsPublic(final ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        try {
            //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
            publicConfigurationTemplateInstanceManager.get(publicResourceUtils.createPublicResourceId(
                    configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(),
                    catalogueId));
        } catch (ResourceException | ResourceNotFoundException e) {
            publicConfigurationTemplateInstanceManager.add(ObjectUtils.clone(configurationTemplateInstanceBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.update(..)) " +
            "&& args(configurationTemplateInstanceBundle,..)", returning = "ret", argNames = "configurationTemplateInstanceBundle,ret")
    public void updatePublicConfigurationTemplateInstance(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, ConfigurationTemplateInstanceBundle ret) {
        try {
            if (!ret.equals(configurationTemplateInstanceBundle)) {
                publicConfigurationTemplateInstanceManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.delete(..))")
    public void deletePublicConfigurationTemplateInstance(JoinPoint joinPoint) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = (ConfigurationTemplateInstanceBundle) joinPoint.getArgs()[0];
        publicConfigurationTemplateInstanceManager.delete(configurationTemplateInstanceBundle);
    }
}