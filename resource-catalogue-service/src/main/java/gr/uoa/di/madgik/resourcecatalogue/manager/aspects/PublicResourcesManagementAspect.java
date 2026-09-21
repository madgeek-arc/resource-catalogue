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
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Profile("beyond")
@Aspect
@Component
public class PublicResourcesManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourcesManagementAspect.class);

    private final TaskExecutor taskExecutor;
    private final PublicOrganisationService publicOrganisationService;
    private final PublicServiceService publicServiceService;
    private final PublicCatalogueService publicCatalogueService;
    private final PublicDatasourceService publicDatasourceService;
    private final PublicTrainingResourceService publicTrainingResourceService;
    private final PublicInteroperabilityRecordService publicGuidelineService;
    private final PublicDeployableApplicationService publicDeployableApplicationService;
    private final PublicAdapterService publicAdapterService;
    private final PublicResourceInteroperabilityRecordService publicRIRService;
    private final PublicConfigurationTemplateInstanceService publicCTIService;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordService;
    private final PublicConfigurationTemplateService publicConfigurationTemplateService;

    public PublicResourcesManagementAspect(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                                           PublicOrganisationService publicOrganisationService,
                                           PublicServiceService publicServiceService,
                                           PublicCatalogueService publicCatalogueService,
                                           PublicDatasourceService publicDatasourceService,
                                           PublicTrainingResourceService publicTrainingResourceService,
                                           PublicInteroperabilityRecordService publicGuidelineService,
                                           PublicDeployableApplicationService publicDeployableApplicationService,
                                           PublicAdapterService publicAdapterService,
                                           PublicResourceInteroperabilityRecordService publicRIRService,
                                           PublicConfigurationTemplateInstanceService publicCTIService,
                                           PublicInteroperabilityRecordService publicInteroperabilityRecordService,
                                           PublicConfigurationTemplateService publicConfigurationTemplateService) {
        this.taskExecutor = taskExecutor;
        this.publicOrganisationService = publicOrganisationService;
        this.publicServiceService = publicServiceService;
        this.publicCatalogueService = publicCatalogueService;
        this.publicDatasourceService = publicDatasourceService;
        this.publicTrainingResourceService = publicTrainingResourceService;
        this.publicGuidelineService = publicGuidelineService;
        this.publicDeployableApplicationService = publicDeployableApplicationService;
        this.publicAdapterService = publicAdapterService;
        this.publicRIRService = publicRIRService;
        this.publicCTIService = publicCTIService;
        this.publicInteroperabilityRecordService = publicInteroperabilityRecordService;
        this.publicConfigurationTemplateService = publicConfigurationTemplateService;
    }

    /**
     * Runs the given task on the async executor only after the enclosing transaction (if any) has
     * committed, so a public-resource sync can never fire on data whose private-side change was
     * rolled back. Falls back to running it immediately if no transaction is active.
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(task);
                }
            });
        } else {
            submit(task);
        }
    }

    /**
     * Submits to the executor without letting a rejection (e.g. pool exhaustion) escape:
     * when called from afterCommit(), an uncaught exception here would propagate out of
     * Spring's commit path and surface to the caller as a failure even though the private-side
     * write already committed successfully.
     */
    private void submit(Runnable task) {
        try {
            taskExecutor.execute(task);
        } catch (RuntimeException e) {
            logger.error("Failed to schedule public-resource sync task", e);
        }
    }

    //region Public Provider
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.verify(..))",
            returning = "bundle")
    public void addPublicProvider(final OrganisationBundle bundle) {
        if (bundle.getStatus().equals("approved") && bundle.isActive()) {
            try {
                publicOrganisationService.get(bundle.getIdentifiers().getPid(), bundle.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicOrganisationService.add(ObjectUtils.clone(bundle), true);
            }
        }
    }

    /**
     * Around aspect which updates the public resource associated with the provided resource.
     *
     * @param pjp      the proceeding join point
     * @param service     the public-layer service of the resource
     * @param resource    the resource that has been updated
     * @param registerPID whether the resource's PID record should be updated on the PID service
     * @param <T>         the type of the resource
     * @return
     * @throws Throwable
     */
    public <T extends Bundle> T updatePublicBundle(ProceedingJoinPoint pjp, PublicResourceService<T> service, T resource, boolean registerPID) throws Throwable {
        T init = ObjectUtils.clone(resource);
        T ret = (T) pjp.proceed();
        try {
            if (!ret.equals(init)) {
                service.update(ObjectUtils.clone(ret), registerPID);
            }
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        return ret;
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.update(..)) && args(provider, ..)")
    public Object updatePublicProvider(ProceedingJoinPoint pjp, OrganisationBundle provider) throws Throwable {
        return updatePublicBundle(pjp, publicOrganisationService, provider, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.setSuspendWithoutCascade(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "bundle")
    public void updatePublicProvider(final OrganisationBundle bundle) {
        runAfterCommit(() -> {
            try {
                publicOrganisationService.update(ObjectUtils.clone(bundle), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Organisation '{}' after update", bundle.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.delete(..))")
    public void deletePublicProvider(JoinPoint joinPoint) {
        OrganisationBundle bundle = (OrganisationBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicOrganisationService.delete(bundle);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Organisation '{}'", bundle.getId(), e);
            }
        });
    }
    //endregion

    //region Public Service
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.finalizeDraft(..))",
            returning = "service")
    public void addPublicService(final ServiceBundle service) {
        if (service.getStatus().equals("approved") && service.isActive()) {
            try {
                publicServiceService.get(service.getIdentifiers().getPid(), service.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicServiceService.add(ObjectUtils.clone(service), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.update(..)) && args(service, ..)")
    public Object updatePublicService(ProceedingJoinPoint pjp, ServiceBundle service) throws Throwable {
        return updatePublicBundle(pjp, publicServiceService, service, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "service")
    public void updatePublicService(final ServiceBundle service) {
        runAfterCommit(() -> {
            try {
                publicServiceService.update(ObjectUtils.clone(service), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Service '{}' after update", service.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.delete(..))")
    public void deletePublicService(JoinPoint joinPoint) {
        ServiceBundle service = (ServiceBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicServiceService.delete(service);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Service '{}'", service.getId(), e);
            }
        });
    }
    //endregion

    //region Public Catalogue
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.finalizeDraft(..))",
            returning = "catalogue")
    public void addPublicCatalogue(final CatalogueBundle catalogue) {
        if (catalogue.getStatus().equals("approved") && catalogue.isActive()) {
            try {
                publicCatalogueService.get(catalogue.getIdentifiers().getPid(), catalogue.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicCatalogueService.add(ObjectUtils.clone(catalogue), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.update(..)) && args(catalogue, ..)")
    public Object updatePublicCatalogue(ProceedingJoinPoint pjp, CatalogueBundle catalogue) throws Throwable {
        return updatePublicBundle(pjp, publicCatalogueService, catalogue, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "catalogue")
    public void updatePublicCatalogue(final CatalogueBundle catalogue) {
        runAfterCommit(() -> {
            try {
                publicCatalogueService.update(ObjectUtils.clone(catalogue), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Catalogue '{}' after update", catalogue.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.delete(..))")
    public void deletePublicCatalogue(JoinPoint joinPoint) {
        CatalogueBundle catalogue = (CatalogueBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicCatalogueService.delete(catalogue);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Catalogue '{}'", catalogue.getId(), e);
            }
        });
    }
    //endregion

    //region Public Datasource
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.finalizeDraft(..))",
            returning = "datasource")
    public void addPublicDatasource(final DatasourceBundle datasource) {
        if (datasource.getStatus().equals("approved") && datasource.isActive()) {
            try {
                publicDatasourceService.get(datasource.getIdentifiers().getPid(), datasource.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicDatasourceService.add(ObjectUtils.clone(datasource), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.update(..)) && args(datasource, ..)")
    public Object updatePublicDatasource(ProceedingJoinPoint pjp, DatasourceBundle datasource) throws Throwable {
        return updatePublicBundle(pjp, publicDatasourceService, datasource, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "datasource")
    public void updatePublicDatasource(final DatasourceBundle datasource) {
        runAfterCommit(() -> {
            try {
                publicDatasourceService.update(ObjectUtils.clone(datasource), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Datasource '{}' after update", datasource.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.delete(..))")
    public void deletePublicDatasource(JoinPoint joinPoint) {
        DatasourceBundle datasource = (DatasourceBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicDatasourceService.delete(datasource);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Datasource '{}'", datasource.getId(), e);
            }
        });
    }
    //endregion

    //region Public Training Resource
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "training")
    public void addPublicTrainingResource(final TrainingResourceBundle training) {
        if (training.getStatus().equals("approved") && training.isActive()) {
            try {
                publicTrainingResourceService.get(training.getIdentifiers().getPid(), training.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicTrainingResourceService.add(ObjectUtils.clone(training), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..)) " +
            "&& args(training,..)")
    public Object updatePublicTrainingResource(ProceedingJoinPoint pjp, TrainingResourceBundle training) throws Throwable {
        return updatePublicBundle(pjp, publicTrainingResourceService, training, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "training")
    public void updatePublicTrainingResource(final TrainingResourceBundle training) {
        runAfterCommit(() -> {
            try {
                publicTrainingResourceService.update(ObjectUtils.clone(training), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Training Resource '{}' after update", training.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.delete(..))")
    public void deletePublicTrainingResource(JoinPoint joinPoint) {
        TrainingResourceBundle training = (TrainingResourceBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicTrainingResourceService.delete(training);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Training Resource '{}'", training.getId(), e);
            }
        });
    }
    //endregion

    //region Public Interoperability Record
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))",
            returning = "guideline")
    public void addPublicGuideline(final InteroperabilityRecordBundle guideline) {
        if (guideline.getStatus().equals("approved") && guideline.isActive()) {
            try {
                publicGuidelineService.get(guideline.getIdentifiers().getPid(), guideline.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicGuidelineService.add(ObjectUtils.clone(guideline), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.update(..)) " +
            "&& args(guideline,..)")
    public Object updatePublicGuideline(ProceedingJoinPoint pjp, InteroperabilityRecordBundle guideline) throws Throwable {
        return updatePublicBundle(pjp, publicInteroperabilityRecordService, guideline, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "guideline")
    public void updatePublicGuideline(final InteroperabilityRecordBundle guideline) {
        runAfterCommit(() -> {
            try {
                publicGuidelineService.update(ObjectUtils.clone(guideline), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Interoperability Record '{}' after update", guideline.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.delete(..))")
    public void deletePublicGuideline(JoinPoint joinPoint) {
        InteroperabilityRecordBundle guideline = (InteroperabilityRecordBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicGuidelineService.delete(guideline);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Interoperability Record '{}'", guideline.getId(), e);
            }
        });
    }
    //endregion

    //region Public Deployable Application
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.verify(..))",
            returning = "deployableApplication")
    public void addPublicDeployableApplication(final DeployableApplicationBundle deployableApplication) {
        if (deployableApplication.getStatus().equals("approved") && deployableApplication.isActive()) {
            try {
                publicDeployableApplicationService.get(deployableApplication.getIdentifiers().getPid(), deployableApplication.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicDeployableApplicationService.add(ObjectUtils.clone(deployableApplication), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.update(..)) " +
            "&& args(deployableApplication,..)")
    public Object updatePublicDeployableApplication(ProceedingJoinPoint pjp, DeployableApplicationBundle deployableApplication) throws Throwable {
        return updatePublicBundle(pjp, publicDeployableApplicationService, deployableApplication, true);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "deployableApplication")
    public void updatePublicDeployableApplication(final DeployableApplicationBundle deployableApplication) {
        runAfterCommit(() -> {
            try {
                publicDeployableApplicationService.update(ObjectUtils.clone(deployableApplication), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Deployable Application '{}' after update", deployableApplication.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.delete(..))")
    public void deletePublicDeployableApplication(JoinPoint joinPoint) {
        DeployableApplicationBundle deployableApplication = (DeployableApplicationBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicDeployableApplicationService.delete(deployableApplication);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Deployable Application '{}'", deployableApplication.getId(), e);
            }
        });
    }
    //endregion

    //region Public Adapter
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.verify(..))",
            returning = "adapter")
    public void addPublicAdapter(final AdapterBundle adapter) {
        if (adapter.getStatus().equals("approved") && adapter.isActive()) {
            try {
                publicAdapterService.get(adapter.getIdentifiers().getPid(), adapter.getCatalogueId());
            } catch (ResourceException | ResourceNotFoundException e) {
                publicAdapterService.add(ObjectUtils.clone(adapter), true);
            }
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.update(..)) && args(adapter,..)")
    public Object updatePublicAdapter(ProceedingJoinPoint pjp, AdapterBundle adapter) throws Throwable {
        return updatePublicBundle(pjp, publicAdapterService, adapter, true);
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.changeResourceOwner(..))")
    public Object updatePublicAdapterOnOwnerChange(ProceedingJoinPoint pjp) throws Throwable {
        AdapterBundle adapter = (AdapterBundle) pjp.proceed();
        try {
            publicAdapterService.update(ObjectUtils.clone(adapter), true);
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        return adapter;
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.setActive(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.setSuspend(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "adapter")
    public void updatePublicAdapter(final AdapterBundle adapter) {
        runAfterCommit(() -> {
            try {
                publicAdapterService.update(ObjectUtils.clone(adapter), null);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to sync Public Adapter '{}' after update", adapter.getId(), e);
            }
        });
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.delete(..))")
    public void deletePublicAdapter(JoinPoint joinPoint) {
        AdapterBundle adapter = (AdapterBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicAdapterService.delete(adapter);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Adapter '{}'", adapter.getId(), e);
            }
        });
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
            publicRIRService.add(ObjectUtils.clone(rir), false);
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.update(..)) " +
            "&& args(rir,..)")
    public Object updatePublicRIR(ProceedingJoinPoint pjp, ResourceInteroperabilityRecordBundle rir) throws Throwable {
        return updatePublicBundle(pjp, publicRIRService, rir, false);
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceInteroperabilityRecordManager.delete(..))")
    public void deletePublicRIR(JoinPoint joinPoint) {
        ResourceInteroperabilityRecordBundle rir = (ResourceInteroperabilityRecordBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicRIRService.delete(rir);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Resource Interoperability Record '{}'", rir.getId(), e);
            }
        });
    }
    //endregion

    //region Public Configuration Template
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.add(..))",
            returning = "ct")
    public void addPublicConfigurationTemplate(final ConfigurationTemplateBundle ct) {
        try {
            publicConfigurationTemplateService.get(ct.getIdentifiers().getPid(), ct.getCatalogueId());
        } catch (ResourceException | ResourceNotFoundException e) {
            publicConfigurationTemplateService.add(ObjectUtils.clone(ct), false);
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.update(..)) " +
            "&& args(ct,..)")
    public Object updatePublicConfigurationTemplate(ProceedingJoinPoint pjp, ConfigurationTemplateBundle ct) throws Throwable {
        return updatePublicBundle(pjp, publicConfigurationTemplateService, ct, false);
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateManager.delete(..))")
    public void deletePublicConfigurationTemplate(JoinPoint joinPoint) {
        ConfigurationTemplateBundle ct = (ConfigurationTemplateBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicConfigurationTemplateService.delete(ct);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Configuration Template '{}'", ct.getId(), e);
            }
        });
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
            publicCTIService.add(ObjectUtils.clone(cti), false);
        }
    }

    @Around("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.update(..)) " +
            "&& args(cti,..)")
    public Object updatePublicCTI(ProceedingJoinPoint pjp, ConfigurationTemplateInstanceBundle cti) throws Throwable {
        return updatePublicBundle(pjp, publicCTIService, cti, false);
    }

    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ConfigurationTemplateInstanceManager.delete(..))")
    public void deletePublicConfigurationTemplateInstance(JoinPoint joinPoint) {
        ConfigurationTemplateInstanceBundle cti = (ConfigurationTemplateInstanceBundle) joinPoint.getArgs()[0];
        runAfterCommit(() -> {
            try {
                publicCTIService.delete(cti);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.error("Failed to delete Public Configuration Template Instance '{}'", cti.getId(), e);
            }
        });
    }
    //endregion
}