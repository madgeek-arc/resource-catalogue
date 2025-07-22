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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class PidServiceRegistrationConsistency {

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    private static final Logger logger = LoggerFactory.getLogger(PidServiceRegistrationConsistency.class);

    private final PidIssuer pidIssuer;

    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final DeployableServiceService deployableServiceService;
    private final AdapterService adapterService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    private final SecurityService securityService;

    public PidServiceRegistrationConsistency(PidIssuer pidIssuer,
                                             ProviderService providerService,
                                             ServiceBundleService<ServiceBundle> serviceBundleService,
                                             TrainingResourceService trainingResourceService,
                                             InteroperabilityRecordService interoperabilityRecordService,
                                             DeployableServiceService deployableServiceService,
                                             AdapterService adapterService,
                                             SecurityService securityService) {
        this.pidIssuer = pidIssuer;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.deployableServiceService = deployableServiceService;
        this.adapterService = adapterService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.securityService = securityService;
    }

//    @Scheduled(cron = "0 0 0 * * *")
//    @Scheduled(initialDelay = 0, fixedRate = 6000)
    protected void postUnregisteredResourcesToPIDService() {
        List<ProviderBundle> allPublicProviders = providerService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<ServiceBundle> allPublicServices = serviceBundleService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allPublicTR = trainingResourceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<DeployableServiceBundle> allPublicDS = deployableServiceService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<InteroperabilityRecordBundle> allPublicIG = interoperabilityRecordService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();
        List<AdapterBundle> allPublicAdapters = adapterService.getAll(createFacetFilter(), securityService.getAdminAccess()).getResults();

        // check consistency for Providers
        for (ProviderBundle providerBundle : allPublicProviders) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(providerBundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                if (pidServiceEnabled) {
                    logger.info("Posting Provider with id {} to PID service", providerBundle.getId());
                    pidIssuer.postPID(providerBundle.getId(), null);
                }
            }
        }

        // check consistency for Services
        for (ServiceBundle serviceBundle : allPublicServices) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(serviceBundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                if (pidServiceEnabled) {
                    logger.info("Posting Service with id {} to PID service", serviceBundle.getId());
                    pidIssuer.postPID(serviceBundle.getId(), null);
                }
            }
        }

        // check consistency for Training Resources
        for (TrainingResourceBundle trainingResourceBundle : allPublicTR) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(trainingResourceBundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                if (pidServiceEnabled) {
                    logger.info("Posting Training Resource with id {} to PID service", trainingResourceBundle.getId());
                    pidIssuer.postPID(trainingResourceBundle.getId(), null);
                }
            }
        }

        // check consistency for Deployable Services
        for (DeployableServiceBundle deployableServiceBundle : allPublicDS) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(deployableServiceBundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                if (pidServiceEnabled) {
                    logger.info("Posting Deployable Service with id {} to PID service", deployableServiceBundle.getId());
                    pidIssuer.postPID(deployableServiceBundle.getId(), null);
                }
            }
        }

        // check consistency for Interoperability Records
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : allPublicIG) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(interoperabilityRecordBundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                if (pidServiceEnabled) {
                    logger.info("Posting Interoperability Record with id {} to PID service", interoperabilityRecordBundle.getId());
                    pidIssuer.postPID(interoperabilityRecordBundle.getId(), null);
                }
            }
        }

        // check consistency for Adapters
        for (AdapterBundle adapterBundle : allPublicAdapters) {
            HttpStatusCode httpStatusCode = getResourceFromPidService(adapterBundle.getId());
            if (httpStatusCode.value() == HttpStatus.NOT_FOUND.value()) {
                if (pidServiceEnabled) {
                    logger.info("Posting Adapter with id {} to PID service", adapterBundle.getId());
                    pidIssuer.postPID(adapterBundle.getId(), null);
                }
            }
        }
    }

    protected FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        return ff;
    }

    protected HttpStatusCode getResourceFromPidService(String pid) {
        return Objects.requireNonNull(pidIssuer.getPidServiceResponse(pid).getStatusCode());
    }

}
