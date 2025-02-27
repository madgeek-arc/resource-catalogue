/**
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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceService extends ResourceManager<TrainingResourceBundle>
        implements PublicResourceService<TrainingResourceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicTrainingResourceService.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;

    public PublicTrainingResourceService(JmsService jmsService,
                                         ProviderResourcesCommonMethods commonMethods,
                                         FacetLabelService facetLabelService) {
        super(TrainingResourceBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceTypeName() {
        return "training_resource";
    }

    @Override
    public Browsing<TrainingResourceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<TrainingResourceBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        String lowerLevelResourceId = trainingResourceBundle.getId();
        Identifiers.createOriginalId(trainingResourceBundle);
        trainingResourceBundle.setId(PublicResourceUtils.createPublicResourceId(trainingResourceBundle.getTrainingResource().getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(trainingResourceBundle.getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId());

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateIdsToPublic(trainingResourceBundle);

        trainingResourceBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : trainingResourceBundle.getTrainingResource().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Training Resource with id '{}' does not have a PID registered under its AlternativeIdentifiers.",
                    trainingResourceBundle.getId());
        } else {
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        TrainingResourceBundle ret;
        logger.info("Training Resource '{}' is being published with id '{}'", lowerLevelResourceId, trainingResourceBundle.getId());
        ret = super.add(trainingResourceBundle, null);
        jmsService.convertAndSendTopic("training_resource.create", trainingResourceBundle);
        return ret;
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        TrainingResourceBundle published = super.get(PublicResourceUtils.createPublicResourceId(
                trainingResourceBundle.getTrainingResource().getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));
        TrainingResourceBundle ret = super.get(PublicResourceUtils.createPublicResourceId(trainingResourceBundle.getTrainingResource().getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, trainingResourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateIdsToPublic(ret);

        ret.getTrainingResource().setAlternativeIdentifiers(published.getTrainingResource().getAlternativeIdentifiers());
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Training Resource with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("training_resource.update", ret);
        return ret;
    }

    @Override
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        try {
            TrainingResourceBundle publicTrainingResourceBundle = get(PublicResourceUtils.createPublicResourceId(
                    trainingResourceBundle.getTrainingResource().getId(),
                    trainingResourceBundle.getTrainingResource().getCatalogueId()));
            logger.info("Deleting public Training Resource with id '{}'", publicTrainingResourceBundle.getId());
            super.delete(publicTrainingResourceBundle);
            jmsService.convertAndSendTopic("training_resource.delete", publicTrainingResourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(TrainingResourceBundle bundle) {
        // Resource Organisation
        bundle.getTrainingResource().setResourceOrganisation(PublicResourceUtils.createPublicResourceId(
                bundle.getTrainingResource().getResourceOrganisation(),
                bundle.getTrainingResource().getCatalogueId()));

        // Resource Providers
        bundle.getTrainingResource().setResourceProviders(
                appendCatalogueId(
                        bundle.getTrainingResource().getResourceProviders(),
                        bundle.getTrainingResource().getCatalogueId()));

        // EOSC Related Services
        bundle.getTrainingResource().setEoscRelatedServices(
                appendCatalogueId(
                        bundle.getTrainingResource().getEoscRelatedServices(),
                        bundle.getTrainingResource().getCatalogueId()));
    }
}
