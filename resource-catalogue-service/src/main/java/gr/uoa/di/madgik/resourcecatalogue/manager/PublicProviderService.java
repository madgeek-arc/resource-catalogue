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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service("publicProviderManager")
public class PublicProviderService extends AbstractPublicResourceManager<ProviderBundle> {

    public PublicProviderService(GenericResourceService genericResourceService,
                                 JmsService jmsService,
                                 PidIssuer pidIssuer,
                                 FacetLabelService facetLabelService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
    }

    @Override
    protected String getResourceTypeName() {
        return "provider";
    }

    @Override
    public void updateIdsToPublic(ProviderBundle bundle) {
        // no-op
    }
}