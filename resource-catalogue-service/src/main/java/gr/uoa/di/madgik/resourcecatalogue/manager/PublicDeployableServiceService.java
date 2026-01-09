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

import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

@Service("publicDeployableServiceManager")
public class PublicDeployableServiceService
        extends AbstractPublicResourceManager<DeployableServiceBundle>
        implements PublicResourceService<DeployableServiceBundle> {

    private final ProviderService providerService;

    public PublicDeployableServiceService(JmsService jmsService,
                                          PidIssuer pidIssuer,
                                          FacetLabelService facetLabelService,
                                          ProviderService providerService) {
        super(DeployableServiceBundle.class, jmsService, pidIssuer, facetLabelService);
        this.providerService = providerService;
    }

    @Override
    public String getResourceTypeName() {
        return "deployable_service";
    }

    @Override
    public void updateIdsToPublic(DeployableServiceBundle bundle) {
        ProviderBundle providerBundle = providerService.get(bundle.getDeployableService().getResourceOrganisation(),
                bundle.getDeployableService().getCatalogueId(), false);
        bundle.getDeployableService().setResourceOrganisation(providerBundle.getIdentifiers().getPid());
    }
}
