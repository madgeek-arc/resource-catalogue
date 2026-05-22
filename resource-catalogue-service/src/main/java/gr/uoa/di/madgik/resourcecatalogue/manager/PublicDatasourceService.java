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

import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicDatasourceManager")
public class PublicDatasourceService extends AbstractPublicResourceManager<DatasourceBundle> {

    private final OrganisationService organisationService;

    public PublicDatasourceService(GenericResourceService genericResourceService,
                                   JmsService jmsService,
                                   PidIssuer pidIssuer,
                                   FacetLabelService facetLabelService,
                                   OrganisationService organisationService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.organisationService = organisationService;
    }

    @Override
    protected String getResourceTypeName() {
        return "datasource";
    }

    @Override
    public void updateIdsToPublic(DatasourceBundle datasource) {
        // Resource Owner
        OrganisationBundle provider = organisationService.get(
                (String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId()
        );
        datasource.getDatasource().put("resourceOwner", provider.getIdentifiers().getPid());

        // Service Providers
        Object providersObj = datasource.getDatasource().get("serviceProviders");
        if (providersObj instanceof List<?> providersList && !providersList.isEmpty()) {
            List<String> updatedServiceProviders = new ArrayList<>();
            for (Object providerObj : providersList) {
                if (providerObj instanceof String providerId) {
                    OrganisationBundle bundle = organisationService.get(providerId, datasource.getCatalogueId());
                    updatedServiceProviders.add(bundle.getIdentifiers().getPid());
                }
            }
            datasource.getDatasource().put("serviceProviders", updatedServiceProviders);
        }
    }
}
