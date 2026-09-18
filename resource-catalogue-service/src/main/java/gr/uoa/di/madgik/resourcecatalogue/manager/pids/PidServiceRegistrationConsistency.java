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

        if (pidServiceEnabled && pidServiceConsistencyEnabled) {
            logger.info("PID Service registration consistency");
            // check consistency for Providers
            postUnregistered(organisationService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(serviceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(catalogueService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(datasourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(trainingResourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(deployableApplicationService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(interoperabilityRecordService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
            postUnregistered(adapterService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults());
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

    private void postUnregistered(List<? extends Bundle> bundles) {
        if (bundles.isEmpty()) {
            return;
        }
        String resourceType = BundleResourceTypes.resolve(bundles.getFirst());
        for (Bundle bundle : bundles) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(bundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                try {
                    logger.info("Posting resource type {} with id {} to PID service", resourceType, bundle.getId());
                    pidIssuer.postPID(bundle, resourceType, null);
                } catch (Exception e) {
                    logger.warn("Failed to post resource type {} with id {} to PID service: {}",
                            resourceType, bundle.getId(), e.getMessage());
                }
            }
        }
    }

}