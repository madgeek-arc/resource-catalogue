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
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicServiceManager")
public class PublicServiceService extends AbstractPublicResourceManager<ServiceBundle> {

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
        return "service";
    }

    @Override
    public void updateIdsToPublic(ServiceBundle service) {
        // Owner
        ProviderBundle provider = providerService.get(
                (String) service.getService().get("owner"),
                service.getCatalogueId()
        );
        service.getService().put("owner", provider.getIdentifiers().getPid());

        // Service Providers
        Object providersObj = service.getService().get("serviceProviders");
        if (providersObj instanceof List<?> providersList && !providersList.isEmpty()) {
            List<String> updatedServiceProviders = new ArrayList<>();
            for (Object providerObj : providersList) {
                if (providerObj instanceof String providerId) {
                    ProviderBundle bundle = providerService.get(providerId, service.getCatalogueId());
                    updatedServiceProviders.add(bundle.getIdentifiers().getPid());
                }
            }
            service.getService().put("serviceProviders", updatedServiceProviders);
        }
    }
}