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

package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;


import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;

//TODO: create different files for different aspect functionality

@Profile("beyond")
@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManagementAspect.class);

    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService guidelineService;
    private final DeployableSoftwareService deployableSoftwareService;
    private final ResourceInteroperabilityRecordService rirService;
    private final ConfigurationTemplateInstanceService ctiService;
    private final PublicProviderService publicProviderService;
    private final PublicServiceService publicServiceService;
    private final PublicDatasourceService publicDatasourceService;
    private final PublicTrainingResourceService publicTrainingResourceService;
    private final PublicInteroperabilityRecordService publicGuidelineService;
    private final PublicDeployableSoftwareService publicDeployableSoftwareService;
    private final PublicAdapterService publicAdapterService;
    private final PublicResourceInteroperabilityRecordService publicRIRService;
    private final PublicConfigurationTemplateInstanceService publicCTIService;
    private final SecurityService securityService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ProviderManagementAspect(ProviderService providerService,
                                    ServiceService serviceService,
                                    DatasourceService datasourceService,
                                    TrainingResourceService trainingResourceService,
                                    InteroperabilityRecordService guidelineService,
                                    DeployableSoftwareService deployableSoftwareService,
                                    ResourceInteroperabilityRecordService rirService,
                                    ConfigurationTemplateInstanceService ctiService,
                                    PublicProviderService publicProviderService,
                                    PublicServiceService publicServiceService,
                                    PublicDatasourceService publicDatasourceService,
                                    PublicTrainingResourceService publicTrainingResourceService,
                                    PublicInteroperabilityRecordService publicGuidelineService,
                                    PublicDeployableSoftwareService publicDeployableSoftwareService,
                                    PublicAdapterService publicAdapterService,
                                    PublicResourceInteroperabilityRecordService publicRIRService,
                                    PublicConfigurationTemplateInstanceService publicCTIService,
                                    SecurityService securityService) {
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.guidelineService = guidelineService;
        this.deployableSoftwareService = deployableSoftwareService;
        this.rirService = rirService;
        this.ctiService = ctiService;
        this.publicProviderService = publicProviderService;
        this.publicServiceService = publicServiceService;
        this.publicDatasourceService = publicDatasourceService;
        this.publicTrainingResourceService = publicTrainingResourceService;
        this.publicAdapterService = publicAdapterService;
        this.publicGuidelineService = publicGuidelineService;
        this.publicDeployableSoftwareService = publicDeployableSoftwareService;
        this.publicRIRService = publicRIRService;
        this.publicCTIService = publicCTIService;
        this.securityService = securityService;
    }

    //region resource state
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))",
            returning = "service")
    public void updatePublicProviderTemplateStatus(final ServiceBundle service) {
        ProviderBundle provider = providerService.get((String) service.getService().get("resourceOwner"),
                service.getCatalogueId());
        publicProviderService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicProviderService.update(provider);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasource")
    public void updatePublicProviderTemplateStatus(final DatasourceBundle datasource) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        publicProviderService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicProviderService.update(provider);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "trainingResource")
    public void updatePublicProviderTemplateStatus(final TrainingResourceBundle trainingResource) {
        ProviderBundle provider = providerService.get((String) trainingResource.getTrainingResource().get("resourceOwner"),
                trainingResource.getCatalogueId());
        publicProviderService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicProviderService.update(provider);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.verify(..))",
            returning = "deployableSoftware")
    public void updatePublicProviderTemplateStatus(final DeployableSoftwareBundle deployableSoftware) {
        ProviderBundle provider = providerService.get((String) deployableSoftware.getDeployableSoftware().get("resourceOwner"),
                deployableSoftware.getCatalogueId());
        publicProviderService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicProviderService.update(provider);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.update(..))",
            returning = "bundle")
    public void updateServiceState(final ServiceBundle bundle) {
        logger.trace("Updating Provider States");
        updateServiceStatus(bundle);
    }

    @Async
    public void updateServiceStatus(ServiceBundle service) {
        if (service.getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle provider = providerService.get((String) service.getService().get("resourceOwner"),
                        service.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    serviceService.verify(service.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.update(..))",
            returning = "bundle")
    public void updateDatasourceState(final DatasourceBundle bundle) {
        logger.trace("Updating Provider States");
        updateDatasourceStatus(bundle);
    }

    @Async
    public void updateDatasourceStatus(DatasourceBundle datasource) {
        if (datasource.getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("resourceOwner"),
                        datasource.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    datasourceService.verify(datasource.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..))",
            returning = "bundle")
    public void updateTrainingResourceState(final TrainingResourceBundle bundle) {
        logger.trace("Updating Provider States");
        updateTrainingResourceStatus(bundle);
    }

    @Async
    public void updateTrainingResourceStatus(TrainingResourceBundle training) {
        if (training.getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle provider = providerService.get((String) training.getTrainingResource().get("resourceOwner"),
                        training.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    trainingResourceService.verify(training.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.update(..))",
            returning = "bundle")
    public void updateDeployableSoftwareState(final DeployableSoftwareBundle bundle) {
        logger.trace("Updating Provider States");
        updateDeployableSoftwareStatus(bundle);
    }

    @Async
    public void updateDeployableSoftwareStatus(DeployableSoftwareBundle bundle) {
        if (bundle.getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle provider = providerService.get((String) bundle.getDeployableSoftware().get("resourceOwner"),
                        bundle.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    deployableSoftwareService.verify(bundle.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    //endregion

    //region registration emails
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.finalizeDraft(..))",
            returning = "bundle")
    public void providerRegistrationEmails(final ProviderBundle bundle) {
        logger.trace("Sending Registration emails");
        if (!bundle.getMetadata().isPublished() && bundle.getCatalogueId().equals(catalogueId)) {
//            emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "providerManager"); //FIXME
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))",
            returning = "service")
    public void providerRegistrationEmails(final ServiceBundle service) {
        ProviderBundle provider = providerService.get((String) service.getService().get("resourceOwner"),
                service.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasource")
    public void providerRegistrationEmails(final DatasourceBundle datasource) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "training")
    public void providerRegistrationEmails(final TrainingResourceBundle training) {
        ProviderBundle provider = providerService.get((String) training.getTrainingResource().get("resourceOwner"),
                training.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.verify(..))",
            returning = "deployableSoftware")
    public void providerRegistrationEmails(final DeployableSoftwareBundle deployableSoftware) {
        ProviderBundle provider = providerService.get((String) deployableSoftware.getDeployableSoftware().get("resourceOwner"),
                deployableSoftware.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }
    //endregion

    //region Public Provider
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))",
            returning = "bundle")
    public void addPublicProvider(final ProviderBundle bundle) {
        if (bundle.getStatus().equals("approved") && bundle.isActive()) {
            try {
                publicProviderService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceException e) {
                publicProviderService.add(ObjectUtils.clone(bundle));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.update(..)) && args(provider, ..)")
    public Object updatePublicProvider(ProceedingJoinPoint pjp, ProviderBundle provider) {
        ProviderBundle init = ObjectUtils.clone(provider);
        ProviderBundle ret = null;
        try {
            ret = (ProviderBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicProviderService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "bundle")
    public void updatePublicProvider(final ProviderBundle bundle) {
        try {
            publicProviderService.update(ObjectUtils.clone(bundle));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.delete(..))")
    public void deletePublicProvider(JoinPoint joinPoint) {
        ProviderBundle bundle = (ProviderBundle) joinPoint.getArgs()[0];
        try {
            publicProviderService.delete(bundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public Service
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.finalizeDraft(..))",
            /*"|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.changeProvider(..))",*/
            returning = "service")
    public void addPublicService(final ServiceBundle service) {
        if (service.getStatus().equals("approved") && service.isActive()) {
            try {
                publicServiceService.get(service.getIdentifiers().getPid(), service.getCatalogueId());
            } catch (ResourceException e) {
                publicServiceService.add(ObjectUtils.clone(service));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.update(..)) && args(service, ..)")
    public Object updatePublicService(ProceedingJoinPoint pjp, ServiceBundle service) {
        ServiceBundle init = ObjectUtils.clone(service);
        ServiceBundle ret = null;
        try {
            ret = (ServiceBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicServiceService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "service")
    public void updatePublicService(final ServiceBundle service) {
        try {
            publicServiceService.update(ObjectUtils.clone(service));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.delete(..))")
    public void deletePublicService(JoinPoint joinPoint) {
        ServiceBundle service = (ServiceBundle) joinPoint.getArgs()[0];
        try {
            publicServiceService.delete(service);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public Datasource
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.finalizeDraft(..))",
            /*"|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.changeProvider(..))",*/
            returning = "datasource")
    public void addPublicDatasource(final DatasourceBundle datasource) {
        if (datasource.getStatus().equals("approved") && datasource.isActive()) {
            try {
                publicDatasourceService.get(datasource.getIdentifiers().getPid(), datasource.getCatalogueId());
            } catch (ResourceException e) {
                publicDatasourceService.add(ObjectUtils.clone(datasource));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.update(..)) && args(datasource, ..)")
    public Object updatePublicDatasource(ProceedingJoinPoint pjp, DatasourceBundle datasource) {
        DatasourceBundle init = ObjectUtils.clone(datasource);
        DatasourceBundle ret = null;
        try {
            ret = (DatasourceBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicDatasourceService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "datasource")
    public void updatePublicDatasource(final DatasourceBundle datasource) {
        try {
            publicDatasourceService.update(ObjectUtils.clone(datasource));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.delete(..))")
    public void deletePublicDatasource(JoinPoint joinPoint) {
        DatasourceBundle datasource = (DatasourceBundle) joinPoint.getArgs()[0];
        try {
            publicDatasourceService.delete(datasource);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public Training Resource
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "training")
    public void addPublicTrainingResource(final TrainingResourceBundle training) {
        if (training.getStatus().equals("approved") && training.isActive()) {
            try {
                publicTrainingResourceService.get(training.getIdentifiers().getPid(), training.getCatalogueId());
            } catch (ResourceException e) {
                publicTrainingResourceService.add(ObjectUtils.clone(training));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..)) " +
            "&& args(training,..)")
    public Object updatePublicTrainingResource(ProceedingJoinPoint pjp, TrainingResourceBundle training) {
        TrainingResourceBundle init = ObjectUtils.clone(training);
        TrainingResourceBundle ret = null;
        try {
            ret = (TrainingResourceBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicTrainingResourceService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.audit(..))",
            returning = "training")
    public void updatePublicTrainingResource(final TrainingResourceBundle training) {
        try {
            publicTrainingResourceService.update(ObjectUtils.clone(training));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.delete(..))")
    public void deletePublicTrainingResource(JoinPoint joinPoint) {
        TrainingResourceBundle training = (TrainingResourceBundle) joinPoint.getArgs()[0];
        try {
            publicTrainingResourceService.delete(training);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public Interoperability Record
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))",
            returning = "guideline")
    public void addPublicGuideline(final InteroperabilityRecordBundle guideline) {
        if (guideline.getStatus().equals("approved") && guideline.isActive()) {
            try {
                publicGuidelineService.get(guideline.getIdentifiers().getPid(), guideline.getCatalogueId());
            } catch (ResourceException e) {
                publicGuidelineService.add(ObjectUtils.clone(guideline));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.update(..)) " +
            "&& args(guideline,..)")
    public Object updatePublicGuideline(ProceedingJoinPoint pjp, InteroperabilityRecordBundle guideline) {
        InteroperabilityRecordBundle init = ObjectUtils.clone(guideline);
        InteroperabilityRecordBundle ret = null;
        try {
            ret = (InteroperabilityRecordBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicGuidelineService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.audit(..))",
            returning = "guideline")
    public void updatePublicGuideline(final InteroperabilityRecordBundle guideline) {
        try {
            publicGuidelineService.update(ObjectUtils.clone(guideline));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.delete(..))")
    public void deletePublicGuideline(JoinPoint joinPoint) {
        InteroperabilityRecordBundle guideline = (InteroperabilityRecordBundle) joinPoint.getArgs()[0];
        try {
            publicGuidelineService.delete(guideline);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public Deployable Software
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.verify(..))",
            returning = "deployableSoftware")
    public void addPublicDeployableSoftware(final DeployableSoftwareBundle deployableSoftware) {
        if (deployableSoftware.getStatus().equals("approved") && deployableSoftware.isActive()) {
            try {
                publicDeployableSoftwareService.get(deployableSoftware.getIdentifiers().getPid(), deployableSoftware.getCatalogueId());
            } catch (ResourceException e) {
                publicDeployableSoftwareService.add(ObjectUtils.clone(deployableSoftware));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.update(..)) " +
            "&& args(deployableSoftware,..)")
    public Object updatePublicDeployableSoftware(ProceedingJoinPoint pjp, DeployableSoftwareBundle deployableSoftware) {
        DeployableSoftwareBundle init = ObjectUtils.clone(deployableSoftware);
        DeployableSoftwareBundle ret = null;
        try {
            ret = (DeployableSoftwareBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicDeployableSoftwareService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.audit(..))",
            returning = "deployableSoftware")
    public void updatePublicDeployableSoftware(final DeployableSoftwareBundle deployableSoftware) {
        try {
            publicDeployableSoftwareService.update(ObjectUtils.clone(deployableSoftware));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableSoftwareManager.delete(..))")
    public void deletePublicDeployableSoftware(JoinPoint joinPoint) {
        DeployableSoftwareBundle deployableSoftware = (DeployableSoftwareBundle) joinPoint.getArgs()[0];
        try {
            publicDeployableSoftwareService.delete(deployableSoftware);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public Adapter
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.verify(..))",
            returning = "adapter")
    public void addPublicAdapter(final AdapterBundle adapter) {
        if (adapter.getStatus().equals("approved") && adapter.isActive()) {
            try {
                publicAdapterService.get(adapter.getIdentifiers().getPid(), adapter.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicAdapterService.add(ObjectUtils.clone(adapter));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.update(..)) && args(adapter,..)")
    public Object updatePublicAdapter(ProceedingJoinPoint pjp, AdapterBundle adapter) {
        AdapterBundle init = ObjectUtils.clone(adapter);
        AdapterBundle ret = null;
        try {
            ret = (AdapterBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicAdapterService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.audit(..))",
            returning = "adapter")
    public void updatePublicAdapter(final AdapterBundle adapter) {
        try {
            publicAdapterService.update(ObjectUtils.clone(adapter));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.delete(..))")
    public void deletePublicAdapter(JoinPoint joinPoint) {
        AdapterBundle adapter = (AdapterBundle) joinPoint.getArgs()[0];
        try {
            publicAdapterService.delete(adapter);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public RIR
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.add(..))",
            returning = "rir")
    public void addPublicRIR(final ResourceInteroperabilityRecordBundle rir) {
        try {
            publicRIRService.get(rir.getIdentifiers().getPid(), rir.getCatalogueId());
        } catch (ResourceException | ResourceNotFoundException e) {
            publicRIRService.add(ObjectUtils.clone(rir));
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.update(..)) " +
            "&& args(rir,..)")
    public Object updatePublicRIR(ProceedingJoinPoint pjp, ResourceInteroperabilityRecordBundle rir) {
        ResourceInteroperabilityRecordBundle init = ObjectUtils.clone(rir);
        ResourceInteroperabilityRecordBundle ret = null;
        try {
            ret = (ResourceInteroperabilityRecordBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicRIRService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.delete(..))")
    public void deletePublicRIR(JoinPoint joinPoint) {
        ResourceInteroperabilityRecordBundle rir = (ResourceInteroperabilityRecordBundle) joinPoint.getArgs()[0];
        try {
            publicRIRService.delete(rir);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion

    //region Public CTI
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.add(..))",
            returning = "cti")
    public void addPublicCTI(final ConfigurationTemplateInstanceBundle cti) {
        try {
            publicCTIService.get(cti.getIdentifiers().getPid(), cti.getCatalogueId());
        } catch (ResourceException | ResourceNotFoundException e) {
            publicCTIService.add(ObjectUtils.clone(cti));
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.update(..)) " +
            "&& args(cti,..)")
    public Object updatePublicCTI(ProceedingJoinPoint pjp, ConfigurationTemplateInstanceBundle cti) {
        ConfigurationTemplateInstanceBundle init = ObjectUtils.clone(cti);
        ConfigurationTemplateInstanceBundle ret = null;
        try {
            ret = (ConfigurationTemplateInstanceBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicCTIService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.delete(..))")
    public void deletePublicConfigurationTemplateInstance(JoinPoint joinPoint) {
        ConfigurationTemplateInstanceBundle cti = (ConfigurationTemplateInstanceBundle) joinPoint.getArgs()[0];
        try {
            publicCTIService.delete(cti);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
    //endregion


    //FIXME
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..)) " +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.add(..))",
//            returning = "catalogueBundle")
//    public void catalogueRegistrationEmails(final CatalogueBundle catalogueBundle) {
//        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToCatalogueAdmins(catalogueBundle);
//    }

    //region extras
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))",
            returning = "service")
    public void assignEoscMonitoringGuidelineToService(final ServiceBundle service) {
        if (service.getStatus().equals("approved")) {
            ResourceInteroperabilityRecordBundle rir = new ResourceInteroperabilityRecordBundle();
            rir.setCatalogueId(service.getCatalogueId());
            rir.getResourceInteroperabilityRecord().put("node", service.getService().get("node"));
            rir.getResourceInteroperabilityRecord().put("resourceId", service.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = guidelineService.getEOSCMonitoringGuideline();
            } catch (Exception e) { //TODO: probably needs ResourceException
                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for Service: {}",
                        service.getId());
                return;
            }

            rir.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", Collections.singletonList(guideline.getId()));
            rirService.add(rir, "service", securityService.getAdminAccess());
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasource")
    public void assignEoscMonitoringGuidelineToDatasource(final DatasourceBundle datasource) {
        if (datasource.getStatus().equals("approved")) {
            ResourceInteroperabilityRecordBundle rir = new ResourceInteroperabilityRecordBundle();
            rir.setCatalogueId(datasource.getCatalogueId());
            rir.getResourceInteroperabilityRecord().put("node", datasource.getDatasource().get("node"));
            rir.getResourceInteroperabilityRecord().put("resourceId", datasource.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = guidelineService.getEOSCMonitoringGuideline();
            } catch (Exception e) { //TODO: probably needs ResourceException
                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for Datasource: {}",
                        datasource.getId());
                return;
            }

            rir.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", Collections.singletonList(guideline.getId()));
            rirService.add(rir, "service", securityService.getAdminAccess());
        }
    }
    //endregion
}