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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

@Service("publicConfigurationTemplateInstanceManager")
public class PublicConfigurationTemplateInstanceService
        extends AbstractPublicResourceManager<ConfigurationTemplateInstanceBundle>
        implements PublicResourceService<ConfigurationTemplateInstanceBundle> {

    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;

    public PublicConfigurationTemplateInstanceService(JmsService jmsService,
                                                      PidIssuer pidIssuer,
                                                      FacetLabelService facetLabelService,
                                                      ServiceService serviceService,
                                                      TrainingResourceService trainingResourceService) {
        super(ConfigurationTemplateInstanceBundle.class, jmsService, pidIssuer, facetLabelService);
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template_instance";
    }

    //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
    public void updateIdsToPublic(ConfigurationTemplateInstanceBundle bundle) {
        // resourceId
        Bundle<?> resourceBundle;
        try {
            resourceBundle = serviceService.get(bundle.getConfigurationTemplateInstance().getResourceId(),
                    bundle.getConfigurationTemplateInstance().getCatalogueId(), false);
        } catch (CatalogueResourceNotFoundException e) {
            resourceBundle = trainingResourceService.get(bundle.getConfigurationTemplateInstance().getResourceId(),
                    bundle.getConfigurationTemplateInstance().getCatalogueId(), false);
        }
        bundle.getConfigurationTemplateInstance().setResourceId(resourceBundle.getIdentifiers().getPid());
    }
}
