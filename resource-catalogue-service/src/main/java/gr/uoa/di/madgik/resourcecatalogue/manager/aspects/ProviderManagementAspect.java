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

package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
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

import java.util.Collections;

@Profile("beyond")
@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManagementAspect.class);

    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService rirService;
    private final PublicProviderService publicProviderService;
    private final PublicServiceService publicServiceManager;
    private final PublicDatasourceService publicDatasourceManager;
    private final PublicTrainingResourceService publicTrainingResourceManager;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordManager;
    private final PublicConfigurationTemplateService publicConfigurationTemplateManager;
    private final PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceManager;
    private final RegistrationMailService registrationMailService;
    private final SecurityService securityService;
    private final PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager;
    private final PublicAdapterService publicAdapterManager;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ProviderManagementAspect(ProviderService providerService,
                                    ServiceBundleService<ServiceBundle> serviceBundleService,
                                    TrainingResourceService trainingResourceService,
                                    InteroperabilityRecordService interoperabilityRecordService,
                                    ResourceInteroperabilityRecordService rirService,
                                    PublicProviderService publicProviderService,
                                    PublicServiceService publicServiceManager,
                                    PublicDatasourceService publicDatasourceManager,
                                    PublicTrainingResourceService publicTrainingResourceManager,
                                    PublicInteroperabilityRecordService publicInteroperabilityRecordManager,
                                    PublicResourceInteroperabilityRecordService publicResourceInteroperabilityRecordManager,
                                    PublicConfigurationTemplateService publicConfigurationTemplateManager,
                                    PublicConfigurationTemplateInstanceService publicConfigurationTemplateInstanceManager,
                                    PublicAdapterService publicAdapterManager,
                                    RegistrationMailService registrationMailService,
                                    SecurityService securityService) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.rirService = rirService;
        this.publicProviderService = publicProviderService;
        this.publicServiceManager = publicServiceManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.publicConfigurationTemplateManager = publicConfigurationTemplateManager;
        this.publicConfigurationTemplateInstanceManager = publicConfigurationTemplateInstanceManager;
        this.publicAdapterManager = publicAdapterManager;
        this.registrationMailService = registrationMailService;
        this.securityService = securityService;
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
            registrationMailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "providerManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.add(..))",
            returning = "catalogueBundle")
    public void catalogueRegistrationEmails(final CatalogueBundle catalogueBundle) {
        logger.trace("Sending Registration emails");
        registrationMailService.sendOnboardingEmailsToCatalogueAdmins(catalogueBundle);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))",
            returning = "serviceBundle")
    public void providerRegistrationEmails(final ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(),
                serviceBundle.getService().getCatalogueId(), false);
        logger.trace("Sending Registration emails");
        registrationMailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager");
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "trainingResourceBundle")
    public void providerRegistrationEmails(final TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
                trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
        logger.trace("Sending Registration emails");
        registrationMailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "trainingResourceManager");
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))",
            returning = "providerBundle")
    public void addProviderAsPublic(final ProviderBundle providerBundle) {
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            try {
                publicProviderService.get(providerBundle.getIdentifiers().getPid(),
                        providerBundle.getProvider().getCatalogueId(), true);
            } catch (CatalogueResourceNotFoundException e) {
                publicProviderService.add(ObjectUtils.clone(providerBundle), null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.update(..)) " +
            "&& args(providerBundle,..)", returning = "ret", argNames = "providerBundle,ret")
    public void updatePublicProvider(ProviderBundle providerBundle, ProviderBundle ret) {
        try {
            if (!ret.equals(providerBundle)) {
                publicProviderService.update(ObjectUtils.clone(ret), null);
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
            publicProviderService.update(ObjectUtils.clone(providerBundle), null);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))",
            returning = "serviceBundle")
    public void updatePublicProviderTemplateStatus(final ServiceBundle serviceBundle) {
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(),
                serviceBundle.getService().getCatalogueId(), false);
        publicProviderService.get(providerBundle.getIdentifiers().getPid(), providerBundle.getProvider().getCatalogueId(), true);
        publicProviderService.update(providerBundle, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "trainingResourceBundle")
    public void updatePublicProviderTemplateStatus(final TrainingResourceBundle trainingResourceBundle) {
        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
                trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
        publicProviderService.get(providerBundle.getIdentifiers().getPid(), providerBundle.getProvider().getCatalogueId(), true);
        publicProviderService.update(providerBundle, null);
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.delete(..))")
    public void deletePublicProvider(JoinPoint joinPoint) {
        ProviderBundle providerBundle = (ProviderBundle) joinPoint.getArgs()[0];
        publicProviderService.delete(providerBundle);
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
                publicServiceManager.get(serviceBundle.getIdentifiers().getPid(),
                        serviceBundle.getService().getCatalogueId(), true);
            } catch (CatalogueResourceNotFoundException e) {
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
                publicTrainingResourceManager.get(trainingResourceBundle.getIdentifiers().getPid(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId(), true);
            } catch (CatalogueResourceNotFoundException e) {
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
                publicInteroperabilityRecordManager.get(interoperabilityRecordBundle.getIdentifiers().getPid(),
                        interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), true);
            } catch (CatalogueResourceNotFoundException e) {
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
                ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(),
                        serviceBundle.getService().getCatalogueId(), false);
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
                ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
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
                publicDatasourceManager.get(datasourceBundle.getIdentifiers().getPid(),
                        datasourceBundle.getDatasource().getCatalogueId(), true);
            } catch (CatalogueResourceNotFoundException e) {
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
            publicResourceInteroperabilityRecordManager.get(
                    resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(), true);
        } catch (CatalogueResourceNotFoundException e) {
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
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.add(..))",
            returning = "configurationTemplateBundle")
    public void addConfigurationTemplateAsPublic(final ConfigurationTemplateBundle configurationTemplateBundle) {
        try {
            //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
            publicConfigurationTemplateManager.get(configurationTemplateBundle.getIdentifiers().getPid(),
                    configurationTemplateBundle.getConfigurationTemplate().getCatalogueId(), true);
        } catch (ResourceException | ResourceNotFoundException e) {
            publicConfigurationTemplateManager.add(ObjectUtils.clone(configurationTemplateBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.update(..)) " +
            "&& args(configurationTemplateBundle,..)", returning = "ret", argNames = "configurationTemplateBundle,ret")
    public void updatePublicConfigurationTemplate(ConfigurationTemplateBundle configurationTemplateBundle, ConfigurationTemplateBundle ret) {
        try {
            if (!ret.equals(configurationTemplateBundle)) {
                publicConfigurationTemplateManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.delete(..))")
    public void deletePublicConfigurationTemplate(JoinPoint joinPoint) {
        ConfigurationTemplateBundle configurationTemplateBundle = (ConfigurationTemplateBundle) joinPoint.getArgs()[0];
        publicConfigurationTemplateManager.delete(configurationTemplateBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.add(..))",
            returning = "configurationTemplateInstanceBundle")
    public void addConfigurationTemplateInstanceAsPublic(final ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        try {
            //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
            publicConfigurationTemplateInstanceManager.get(configurationTemplateInstanceBundle.getIdentifiers().getPid(),
                    configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getCatalogueId(), true);
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

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.add(..))",
            returning = "adapterBundle")
    public void addAdapterAsPublic(final AdapterBundle adapterBundle) {
        try {
            //TODO: Refactor if Adapters can belong to a different from the Project's Catalogue
            publicAdapterManager.get(adapterBundle.getIdentifiers().getPid(),
                    adapterBundle.getAdapter().getCatalogueId(), true);
        } catch (ResourceException | ResourceNotFoundException e) {
            publicAdapterManager.add(ObjectUtils.clone(adapterBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.update(..)) " +
            "&& args(adapterBundle,..)", returning = "ret", argNames = "adapterBundle,ret")
    public void updatePublicAdapter(AdapterBundle adapterBundle, AdapterBundle ret) {
        try {
            if (!ret.equals(adapterBundle)) {
                publicAdapterManager.update(ObjectUtils.clone(ret), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.delete(..))")
    public void deletePublicAdapter(JoinPoint joinPoint) {
        AdapterBundle adapterBundle = (AdapterBundle) joinPoint.getArgs()[0];
        publicAdapterManager.delete(adapterBundle);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.addResource(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceBundleManager.verify(..))",
            returning = "serviceBundle")
    public void assignEoscMonitoringGuidelineToService(final ServiceBundle serviceBundle) {
        if (serviceBundle.getStatus().equals("approved resource")) {
            ResourceInteroperabilityRecord rir = new ResourceInteroperabilityRecord();
            rir.setCatalogueId(serviceBundle.getService().getCatalogueId());
            rir.setNode(serviceBundle.getService().getNode());
            rir.setResourceId(serviceBundle.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = interoperabilityRecordService.getEOSCMonitoringGuideline();
            } catch (CatalogueResourceNotFoundException e) {
                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for service: {}",
                        serviceBundle.getId());
                return;
            }

            rir.setInteroperabilityRecordIds(Collections.singletonList(guideline.getId()));
            rirService.add(new ResourceInteroperabilityRecordBundle(rir),
                    "service", securityService.getAdminAccess());
        }
    }
}