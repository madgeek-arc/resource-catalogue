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

//TODO: create different files for different aspect functionality

@Profile("beyond")
@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManagementAspect.class);

    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final InteroperabilityRecordService guidelineService;
    private final DeployableServiceService deployableServiceService;
    private final PublicProviderService publicProviderService;
    private final PublicServiceService publicServiceService;
    private final PublicDatasourceService publicDatasourceService;
    private final PublicAdapterService publicAdapterService;
    private final PublicInteroperabilityRecordService publicGuidelineService;
    private final PublicDeployableServiceService publicDeployableServiceService;
    private final SecurityService securityService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ProviderManagementAspect(ProviderService providerService,
                                    ServiceService serviceService,
                                    DatasourceService datasourceService,
                                    InteroperabilityRecordService guidelineService,
                                    DeployableServiceService deployableServiceService,
                                    PublicProviderService publicProviderService,
                                    PublicServiceService publicServiceService,
                                    PublicDatasourceService publicDatasourceService,
                                    PublicAdapterService publicAdapterService,
                                    PublicInteroperabilityRecordService publicGuidelineService,
                                    PublicDeployableServiceService publicDeployableServiceService,
                                    SecurityService securityService) {
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.guidelineService = guidelineService;
        this.deployableServiceService = deployableServiceService;
        this.publicProviderService = publicProviderService;
        this.publicServiceService = publicServiceService;
        this.publicDatasourceService = publicDatasourceService;
        this.publicAdapterService = publicAdapterService;
        this.publicGuidelineService = publicGuidelineService;
        this.publicDeployableServiceService = publicDeployableServiceService;
        this.securityService = securityService;
    }

    //region resource state
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setStatus(..))",
            returning = "service")
    public void updatePublicProviderTemplateStatus(final ServiceBundle service) {
        ProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        publicProviderService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicProviderService.update(provider);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setStatus(..))",
            returning = "datasource")
    public void updatePublicProviderTemplateStatus(final DatasourceBundle datasource) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("serviceOwner"),
                datasource.getCatalogueId());
        publicProviderService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicProviderService.update(provider);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.setStatus(..))",
            returning = "deployableService")
    public void updatePublicProviderTemplateStatus(final DeployableServiceBundle deployableService) {
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("serviceOwner"),
                deployableService.getCatalogueId());
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
                ProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                        service.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    serviceService.setStatus(service.getId(), "pending", false, securityService.getAdminAccess());
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
                ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("serviceOwner"),
                        datasource.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    datasourceService.setStatus(datasource.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.update(..))",
            returning = "bundle")
    public void updateDeployableServiceState(final DeployableServiceBundle bundle) {
        logger.trace("Updating Provider States");
        updateDeployableServiceStatus(bundle);
    }

    @Async
    public void updateDeployableServiceStatus(DeployableServiceBundle bundle) {
        if (bundle.getCatalogueId().equals(catalogueId)) {
            try {
                ProviderBundle provider = providerService.get((String) bundle.getDeployableService().get("serviceOwner"),
                        bundle.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    deployableServiceService.setStatus(bundle.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    //endregion

    //region registration emails
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.setStatus(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.finalizeDraft(..))",
            returning = "bundle")
    public void providerRegistrationEmails(final ProviderBundle bundle) {
        logger.trace("Sending Registration emails");
        if (!bundle.getMetadata().isPublished() && bundle.getCatalogueId().equals(catalogueId)) {
//            emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "providerManager"); //FIXME
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setStatus(..))",
            returning = "service")
    public void providerRegistrationEmails(final ServiceBundle service) {
        ProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setStatus(..))",
            returning = "datasource")
    public void providerRegistrationEmails(final DatasourceBundle datasource) {
        ProviderBundle provider = providerService.get((String) datasource.getDatasource().get("serviceOwner"),
                datasource.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.setStatus(..))",
            returning = "deployableService")
    public void providerRegistrationEmails(final DeployableServiceBundle deployableService) {
        ProviderBundle provider = providerService.get((String) deployableService.getDeployableService().get("serviceOwner"),
                deployableService.getCatalogueId());
        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "serviceBundleManager"); //FIXME
    }
    //endregion

    //region Public Provider
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.setStatus(..))",
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
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager.setStatus(..))" +
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
        publicProviderService.delete(bundle);
    }
    //endregion

    //region Public Service
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setStatus(..))" +
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
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setStatus(..))" +
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
        publicServiceService.delete(service);
    }
    //endregion

    //region Public Datasource
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setStatus(..))" +
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
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setStatus(..))" +
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
        publicDatasourceService.delete(datasource);
    }
    //endregion

    //region Public Adapter
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.setStatus(..))",
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
    public Object updatePublicResource(ProceedingJoinPoint pjp, AdapterBundle adapter) {
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
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.setStatus(..))" +
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
        publicAdapterService.delete(adapter);
    }
    //endregion

    //region Public Interoperability Record
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.setStatus(..))",
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
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.setStatus(..))" +
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
        publicGuidelineService.delete(guideline);
    }
    //endregion

    //region Public Deployable Service
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.setStatus(..))",
            returning = "deployableService")
    public void addPublicDeployableService(final DeployableServiceBundle deployableService) {
        if (deployableService.getStatus().equals("approved") && deployableService.isActive()) {
            try {
                publicDeployableServiceService.get(deployableService.getIdentifiers().getPid(), deployableService.getCatalogueId());
            } catch (ResourceException e) {
                publicDeployableServiceService.add(ObjectUtils.clone(deployableService));
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.update(..)) " +
            "&& args(deployableService,..)")
    public Object updatePublicDeployableService(ProceedingJoinPoint pjp, DeployableServiceBundle deployableService) {
        DeployableServiceBundle init = ObjectUtils.clone(deployableService);
        DeployableServiceBundle ret = null;
        try {
            ret = (DeployableServiceBundle) pjp.proceed();
            if (!ret.equals(init)) {
                publicDeployableServiceService.update(ObjectUtils.clone(ret));
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.setStatus(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.audit(..))",
            returning = "deployableService")
    public void updatePublicDeployableService(final DeployableServiceBundle deployableService) {
        try {
            publicDeployableServiceService.update(ObjectUtils.clone(deployableService));
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableServiceManager.delete(..))")
    public void deletePublicDeployableService(JoinPoint joinPoint) {
        DeployableServiceBundle deployableService = (DeployableServiceBundle) joinPoint.getArgs()[0];
        publicDeployableServiceService.delete(deployableService);
    }
    //endregion

//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftTrainingResourceManager.transformToNonDraft(..)) " +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.add(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..))",
//            returning = "trainingResourceBundle")
//    public void updateProviderState(final TrainingResourceBundle trainingResourceBundle) {
//        logger.trace("Updating Provider States");
//        updateTrainingResourceProviderStates(trainingResourceBundle);
//    }

//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..)) " +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.add(..))",
//            returning = "catalogueBundle")
//    public void catalogueRegistrationEmails(final CatalogueBundle catalogueBundle) {
//        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToCatalogueAdmins(catalogueBundle);
//    }

//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
//            returning = "trainingResourceBundle")
//    public void providerRegistrationEmails(final TrainingResourceBundle trainingResourceBundle) {
//        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
//                trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
//        logger.trace("Sending Registration emails");
//        emailService.sendOnboardingEmailsToProviderAdmins(providerBundle, "trainingResourceManager");
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
//            returning = "trainingResourceBundle")
//    public void updatePublicProviderTemplateStatus(final TrainingResourceBundle trainingResourceBundle) {
//        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
//                trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
//        publicProviderService.get(providerBundle.getIdentifiers().getPid(), providerBundle.getProvider().getCatalogueId(), true);
//        publicProviderService.update(providerBundle, null);
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.add(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DraftTrainingResourceManager.transformToNonDraft(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.changeProvider(..))",
//            returning = "trainingResourceBundle")
//    public void addResourceAsPublic(final TrainingResourceBundle trainingResourceBundle) {
//        if (trainingResourceBundle.getStatus().equals("approved") && trainingResourceBundle.isActive()) {
//            try {
//                publicTrainingResourceManager.get(trainingResourceBundle.getIdentifiers().getPid(),
//                        trainingResourceBundle.getTrainingResource().getCatalogueId(), true);
//            } catch (ResourceException e) {
//                publicTrainingResourceManager.add(ObjectUtils.clone(trainingResourceBundle), null);
//            }
//        }
//    }

//    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..)) " +
//            "&& args(trainingResourceBundle,..)")
//    public Object updatePublicResource(ProceedingJoinPoint pjp, TrainingResourceBundle trainingResourceBundle) {
//        TrainingResourceBundle init = ObjectUtils.clone(trainingResourceBundle);
//        TrainingResourceBundle ret = null;
//        try {
//            ret = (TrainingResourceBundle) pjp.proceed();
//            if (!ret.equals(init)) {
//                publicTrainingResourceManager.update(ObjectUtils.clone(ret), null);
//            }
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return ret;
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.publish(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.suspend(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.audit(..))",
//            returning = "trainingResourceBundle")
//    public void updatePublicResource(final TrainingResourceBundle trainingResourceBundle) {
//        try {
//            publicTrainingResourceManager.update(ObjectUtils.clone(trainingResourceBundle), null);
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        }
//    }

//    @Async
//    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.delete(..))")
//    public void deletePublicTrainingResource(JoinPoint joinPoint) {
//        TrainingResourceBundle trainingResourceBundle = (TrainingResourceBundle) joinPoint.getArgs()[0];
//        publicTrainingResourceManager.delete(trainingResourceBundle);
//    }

//    @Async
//    public void updateTrainingResourceProviderStates(TrainingResourceBundle trainingResourceBundle) {
//        if (trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
//            try {
//                ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
//                        trainingResourceBundle.getTrainingResource().getCatalogueId(), false);
//                if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
//                    logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
//                            trainingResourceBundle.getTrainingResource().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
//                    trainingResourceService.verify(trainingResourceBundle.getTrainingResource().getId(), "pending", false, securityService.getAdminAccess());
//                }
//            } catch (RuntimeException e) {
//                logger.error(e.getMessage(), e);
//            }
//        }
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
//            returning = "datasourceBundle")
//    public void addDatasourceAsPublic(final DatasourceBundle datasourceBundle) {
//        if (datasourceBundle.getStatus().equals("approved") && datasourceBundle.isActive()) {
//            try {
//                publicDatasourceManager.get(datasourceBundle.getIdentifiers().getPid(),
//                        datasourceBundle.getDatasource().getCatalogueId(), true);
//            } catch (ResourceException e) {
//                publicDatasourceManager.add(ObjectUtils.clone(datasourceBundle), null);
//            }
//        }
//    }

//    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.update(..)) " +
//            "&& args(datasourceBundle,..)")
//    public Object updatePublicResource(ProceedingJoinPoint pjp, DatasourceBundle datasourceBundle) {
//        DatasourceBundle init = ObjectUtils.clone(datasourceBundle);
//        DatasourceBundle ret = null;
//        try {
//            ret = (DatasourceBundle) pjp.proceed();
//            if (!ret.equals(init)) {
//                publicDatasourceManager.update(ObjectUtils.clone(ret), null);
//            }
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return ret;
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
//            returning = "datasourceBundle")
//    public void updatePublicDatasource(final DatasourceBundle datasourceBundle) {
//        try {
//            publicDatasourceManager.update(ObjectUtils.clone(datasourceBundle), null);
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        }
//    }

//    @Async
//    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.delete(..))")
//    public void deletePublicDatasource(JoinPoint joinPoint) {
//        DatasourceBundle datasourceBundle = (DatasourceBundle) joinPoint.getArgs()[0];
//        publicDatasourceManager.delete(datasourceBundle);
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.add(..))",
//            returning = "resourceInteroperabilityRecordBundle")
//    public void addResourceInteroperabilityRecordAsPublic(final ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
//        // TODO: check Resource states (publish if only approved/active)
//        try {
//            publicResourceInteroperabilityRecordManager.get(
//                    resourceInteroperabilityRecordBundle.getIdentifiers().getPid(),
//                    resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(), true);
//        } catch (ResourceException e) {
//            publicResourceInteroperabilityRecordManager.add(ObjectUtils.clone(resourceInteroperabilityRecordBundle), null);
//        }
//    }

//    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.update(..)) " +
//            "&& args(resourceInteroperabilityRecordBundle,..)")
//    public Object updatePublicResource(ProceedingJoinPoint pjp, ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
//        ResourceInteroperabilityRecordBundle init = ObjectUtils.clone(resourceInteroperabilityRecordBundle);
//        ResourceInteroperabilityRecordBundle ret = null;
//        try {
//            ret = (ResourceInteroperabilityRecordBundle) pjp.proceed();
//            if (!ret.equals(init)) {
//                publicResourceInteroperabilityRecordManager.update(ObjectUtils.clone(ret), null);
//            }
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return ret;
//    }

//    @Async
//    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.delete(..))")
//    public void deletePublicResourceInteroperabilityRecord(JoinPoint joinPoint) {
//        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = (ResourceInteroperabilityRecordBundle) joinPoint.getArgs()[0];
//        publicResourceInteroperabilityRecordManager.delete(resourceInteroperabilityRecordBundle);
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.add(..))",
//            returning = "configurationTemplateBundle")
//    public void addConfigurationTemplateAsPublic(final ConfigurationTemplateBundle configurationTemplateBundle) {
//        try {
//            //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
//            publicConfigurationTemplateManager.get(configurationTemplateBundle.getIdentifiers().getPid(),
//                    configurationTemplateBundle.getConfigurationTemplate().getCatalogueId(), true);
//        } catch (ResourceException | ResourceNotFoundException e) {
//            publicConfigurationTemplateManager.add(ObjectUtils.clone(configurationTemplateBundle), null);
//        }
//    }

//    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.update(..)) " +
//            "&& args(configurationTemplateBundle,..)")
//    public Object updatePublicResource(ProceedingJoinPoint pjp, ConfigurationTemplateBundle configurationTemplateBundle) {
//        ConfigurationTemplateBundle init = ObjectUtils.clone(configurationTemplateBundle);
//        ConfigurationTemplateBundle ret = null;
//        try {
//            ret = (ConfigurationTemplateBundle) pjp.proceed();
//            if (!ret.equals(init)) {
//                publicConfigurationTemplateManager.update(ObjectUtils.clone(ret), null);
//            }
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return ret;
//    }

//    @Async
//    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.delete(..))")
//    public void deletePublicConfigurationTemplate(JoinPoint joinPoint) {
//        ConfigurationTemplateBundle configurationTemplateBundle = (ConfigurationTemplateBundle) joinPoint.getArgs()[0];
//        publicConfigurationTemplateManager.delete(configurationTemplateBundle);
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.add(..))",
//            returning = "configurationTemplateInstanceBundle")
//    public void addConfigurationTemplateInstanceAsPublic(final ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
//        try {
//            //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
//            publicConfigurationTemplateInstanceManager.get(configurationTemplateInstanceBundle.getIdentifiers().getPid(),
//                    configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getCatalogueId(), true);
//        } catch (ResourceException | ResourceNotFoundException e) {
//            publicConfigurationTemplateInstanceManager.add(ObjectUtils.clone(configurationTemplateInstanceBundle), null);
//        }
//    }

//    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.update(..)) " +
//            "&& args(configurationTemplateInstanceBundle,..)")
//    public Object updatePublicResource(ProceedingJoinPoint pjp, ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
//        ConfigurationTemplateInstanceBundle init = ObjectUtils.clone(configurationTemplateInstanceBundle);
//        ConfigurationTemplateInstanceBundle ret = null;
//        try {
//            ret = (ConfigurationTemplateInstanceBundle) pjp.proceed();
//            if (!ret.equals(init)) {
//                publicConfigurationTemplateInstanceManager.update(ObjectUtils.clone(ret), null);
//            }
//        } catch (ResourceException | ResourceNotFoundException ignore) {
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return ret;
//    }

//    @Async
//    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.delete(..))")
//    public void deletePublicConfigurationTemplateInstance(JoinPoint joinPoint) {
//        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = (ConfigurationTemplateInstanceBundle) joinPoint.getArgs()[0];
//        publicConfigurationTemplateInstanceManager.delete(configurationTemplateInstanceBundle);
//    }

//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.add(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setStatus(..))",
//            returning = "service")
//    public void assignEoscMonitoringGuidelineToService(final ServiceBundle service) {
//        if (service.getStatus().equals("approved")) {
//            ResourceInteroperabilityRecord rir = new ResourceInteroperabilityRecord();
//            rir.setCatalogueId(service.getCatalogueId());
//            rir.setNode((String) service.getService().get("node"));
//            rir.setResourceId(service.getId());
//
//            InteroperabilityRecordBundle guideline;
//            try {
//                guideline = interoperabilityRecordService.getEOSCMonitoringGuideline();
//            } catch (CatalogueResourceNotFoundException e) { //TODO: probably needs ResourceException
//                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for service: {}",
//                        service.getId());
//                return;
//            }
//
//            rir.setInteroperabilityRecordIds(Collections.singletonList(guideline.getId()));
//            rirService.add(new ResourceInteroperabilityRecordBundle(rir),
//                    "service", securityService.getAdminAccess());
//        }
//    }
//
//    @Async
//    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
//            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setStatus(..))",
//            returning = "datasource")
//    public void assignEoscMonitoringGuidelineToService(final DatasourceBundle datasource) {
//        if (datasource.getStatus().equals("approved")) {
//            ResourceInteroperabilityRecord rir = new ResourceInteroperabilityRecord();
//            rir.setCatalogueId(datasource.getCatalogueId());
//            rir.setNode((String) datasource.getDatasource().get("node"));
//            rir.setResourceId(datasource.getId());
//
//            InteroperabilityRecordBundle guideline;
//            try {
//                guideline = interoperabilityRecordService.getEOSCMonitoringGuideline();
//            } catch (CatalogueResourceNotFoundException e) { //TODO: probably needs ResourceException
//                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for service: {}",
//                        service.getId());
//                return;
//            }
//
//            rir.setInteroperabilityRecordIds(Collections.singletonList(guideline.getId()));
//            rirService.add(new ResourceInteroperabilityRecordBundle(rir),
//                    "service", securityService.getAdminAccess());
//        }
//    }
}