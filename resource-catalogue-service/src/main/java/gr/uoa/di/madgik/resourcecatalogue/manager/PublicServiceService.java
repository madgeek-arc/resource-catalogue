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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicServiceManager")
public class PublicServiceService extends AbstractPublicResourceManager<NewServiceBundle> {

    private final ProviderService providerService;

    public PublicServiceService(GenericResourceService genericResourceService,
                                JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService,
                                ProviderService providerService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.providerService = providerService;
    }

    @Override
    protected String getResourceTypeName() {
        return "servicetest";
    }

    @Override
    public void updateIdsToPublic(NewServiceBundle bundle) {
        // Service Owner
        NewProviderBundle providerBundle = providerService.get(
                (String) bundle.getService().get("serviceOwner"),
                bundle.getCatalogueId()
        );
        bundle.getService().put("serviceOwner", providerBundle.getIdentifiers().getPid());

        // Service Providers
        Object providersObj = bundle.getService().get("serviceProviders");
        if (providersObj instanceof List<?> providersList && !providersList.isEmpty()) {
            List<String> updatedServiceProviders = new ArrayList<>();
            for (Object providerObj : providersList) {
                if (providerObj instanceof String providerId) {
                    NewProviderBundle resourceProvider = providerService.get(providerId, bundle.getCatalogueId());
                    updatedServiceProviders.add(resourceProvider.getIdentifiers().getPid());
                }
            }
            bundle.getService().put("serviceProviders", updatedServiceProviders);
        }
    }
}