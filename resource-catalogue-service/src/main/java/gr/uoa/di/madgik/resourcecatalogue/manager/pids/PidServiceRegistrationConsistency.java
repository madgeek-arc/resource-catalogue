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

package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class PidServiceRegistrationConsistency {

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;
    @Value("${pid.service.consistency.enabled}")
    private boolean pidServiceConsistencyEnabled;

    private static final Logger logger = LoggerFactory.getLogger(PidServiceRegistrationConsistency.class);

    private final PidIssuer pidIssuer;

    private final PublicOrganisationService organisationService;
    private final PublicServiceService serviceService;
    private final PublicCatalogueService catalogueService;
    private final PublicDatasourceService datasourceService;
    private final PublicTrainingResourceService trainingResourceService;
    private final PublicDeployableApplicationService deployableApplicationService;
    private final PublicAdapterService adapterService;
    private final PublicInteroperabilityRecordService interoperabilityRecordService;

    private final SecurityService securityService;

    public PidServiceRegistrationConsistency(PidIssuer pidIssuer,
                                             PublicOrganisationService organisationService,
                                             PublicServiceService serviceService,
                                             PublicCatalogueService catalogueService,
                                             PublicDatasourceService datasourceService,
                                             PublicTrainingResourceService trainingResourceService,
                                             PublicInteroperabilityRecordService interoperabilityRecordService,
                                             PublicDeployableApplicationService deployableApplicationService,
                                             PublicAdapterService adapterService,
                                             SecurityService securityService) {
        this.pidIssuer = pidIssuer;
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.catalogueService = catalogueService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.deployableApplicationService = deployableApplicationService;
        this.adapterService = adapterService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.securityService = securityService;
    }

    @Scheduled(cron = "0 0 0 * * *")
//    @Scheduled(initialDelay = 0, fixedRate = 6000)
    protected void postUnregisteredResourcesToPIDService() {
        List<OrganisationBundle> allPublicOrganisations = organisationService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<ServiceBundle> allPublicServices = serviceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<CatalogueBundle> allPublicCatalogues = catalogueService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<DatasourceBundle> allPublicDatasources = datasourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allPublicTR = trainingResourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<DeployableApplicationBundle> allPublicDS = deployableApplicationService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<InteroperabilityRecordBundle> allPublicIG = interoperabilityRecordService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<AdapterBundle> allPublicAdapters = adapterService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();

        if (pidServiceEnabled && pidServiceConsistencyEnabled) {
            logger.info("PID Service registration consistency");
            // check consistency for Providers
            for (OrganisationBundle organisationBundle : allPublicOrganisations) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(organisationBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Provider with id {} to PID service", organisationBundle.getId());
                    pidIssuer.postPID(organisationBundle.getId(), null);
                }
            }

            // check consistency for Adapters
            for (AdapterBundle adapterBundle : allPublicAdapters) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(adapterBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Adapter with id {} to PID service", adapterBundle.getId());
                    pidIssuer.postPID(adapterBundle.getId(), null);
                }
            }

            // check consistency for Services
            for (ServiceBundle serviceBundle : allPublicServices) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(serviceBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Service with id {} to PID service", serviceBundle.getId());
                    pidIssuer.postPID(serviceBundle.getId(), null);
                }
            }

            // check consistency for Catalogues
            for (CatalogueBundle catalogueBundle : allPublicCatalogues) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(catalogueBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Catalogue with id {} to PID service", catalogueBundle.getId());
                    pidIssuer.postPID(catalogueBundle.getId(), null);
                }
            }

            // check consistency for Datasources
            for (DatasourceBundle datasourceBundle : allPublicDatasources) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(datasourceBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Datasource with id {} to PID service", datasourceBundle.getId());
                    pidIssuer.postPID(datasourceBundle.getId(), null);
                }
            }

            // check consistency for Training Resources
            for (TrainingResourceBundle trainingResourceBundle : allPublicTR) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(trainingResourceBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Training Resource with id {} to PID service", trainingResourceBundle.getId());
                    pidIssuer.postPID(trainingResourceBundle.getId(), null);
                }
            }

            // check consistency for Deployable Application
            for (DeployableApplicationBundle deployableApplicationBundle : allPublicDS) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(deployableApplicationBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Deployable Application with id {} to PID service", deployableApplicationBundle.getId());
                    pidIssuer.postPID(deployableApplicationBundle.getId(), null);
                }
            }

            // check consistency for Interoperability Records
            for (InteroperabilityRecordBundle interoperabilityRecordBundle : allPublicIG) {
                HttpStatusCode httpStatusCode = getResourceFromPidService(interoperabilityRecordBundle.getId());
                if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("Posting Interoperability Record with id {} to PID service", interoperabilityRecordBundle.getId());
                    pidIssuer.postPID(interoperabilityRecordBundle.getId(), null);
                }
            }
        }
    }

    protected FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return ff;
    }

    protected HttpStatusCode getResourceFromPidService(String pid) {
        return Objects.requireNonNull(pidIssuer.getPidServiceResponse(pid).getStatusCode());
    }

}