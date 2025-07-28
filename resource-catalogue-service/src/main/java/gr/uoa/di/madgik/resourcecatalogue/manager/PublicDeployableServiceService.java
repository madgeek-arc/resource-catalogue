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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicDeployableServiceManager")
public class PublicDeployableServiceService extends ResourceCatalogueManager<DeployableServiceBundle>
        implements PublicResourceService<DeployableServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicDeployableServiceService.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;
    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    public PublicDeployableServiceService(JmsService jmsService,
                                          PidIssuer pidIssuer,
                                          FacetLabelService facetLabelService,
                                          ProviderService providerService,
                                          ServiceBundleService<ServiceBundle> serviceBundleService,
                                          TrainingResourceService trainingResourceService) {
        super(DeployableServiceBundle.class);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.facetLabelService = facetLabelService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "deployable_service";
    }

    @Override
    public Browsing<DeployableServiceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<DeployableServiceBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public DeployableServiceBundle add(DeployableServiceBundle bundle, Authentication authentication) {
        String lowerLevelResourceId = bundle.getId();
        bundle.setId(bundle.getIdentifiers().getPid());
        bundle.getMetadata().setPublished(true);

        // set public id to resource organization
        updateIdsToPublic(bundle);

        // POST PID
        if (pidServiceEnabled) {
            logger.info("Posting Deployable Service with id {} to PID service", bundle.getId());
            pidIssuer.postPID(bundle.getId(), null);
        }

        DeployableServiceBundle ret;
        logger.info("Deployable Service '{}' is being published with id '{}'", lowerLevelResourceId, bundle.getId());
        ret = super.add(bundle, null);
        jmsService.convertAndSendTopic("deployable_service.create", bundle);
        return ret;
    }

    @Override
    public DeployableServiceBundle update(DeployableServiceBundle bundle, Authentication authentication) {
        DeployableServiceBundle published = super.get(bundle.getIdentifiers().getPid(), bundle.getDeployableService().getCatalogueId(), true);
        DeployableServiceBundle ret = super.get(bundle.getIdentifiers().getPid(), bundle.getDeployableService().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, bundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // set public id to resource organization
        updateIdsToPublic(ret);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Deployable Service with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("deployable_service.update", ret);
        return ret;
    }

    @Override
    public void delete(DeployableServiceBundle bundle) {
        try {
            DeployableServiceBundle publicBundle = get(bundle.getIdentifiers().getPid(),
                    bundle.getDeployableService().getCatalogueId(), true);
            logger.info("Deleting public Deployable Service with id '{}'", publicBundle.getId());
            super.delete(publicBundle);
            jmsService.convertAndSendTopic("deployable_service.delete", bundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(DeployableServiceBundle bundle) {
        ProviderBundle providerBundle = providerService.get(bundle.getDeployableService().getResourceOrganisation(),
                bundle.getDeployableService().getCatalogueId(), false);
        bundle.getDeployableService().setResourceOrganisation(providerBundle.getIdentifiers().getPid());
    }
}
