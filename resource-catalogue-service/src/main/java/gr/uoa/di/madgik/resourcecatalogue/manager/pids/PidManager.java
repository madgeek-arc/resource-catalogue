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

package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.PidService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PidManager implements PidService {

    private final PidIssuer pidIssuer;
    private final GenericResourceService genericResourceService;
    private final CatalogueProperties catalogueProperties;

    public PidManager(PidIssuer pidIssuer,
                      GenericResourceService genericResourceService,
                      CatalogueProperties catalogueProperties) {
        this.pidIssuer = pidIssuer;
        this.genericResourceService = genericResourceService;
        this.catalogueProperties = catalogueProperties;
    }

    @Override
    public Bundle<?> get(String prefix, String suffix) {
        String pid = prefix + "/" + suffix;
        String resourceType = catalogueProperties.getResourceTypeFromPrefix(prefix);
        if (resourceType != null) {
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(10000);
            ff.setResourceType(resourceType);
            ff.addFilter("resource_internal_id", pid);
            Browsing<Bundle<?>> browsing = genericResourceService.getResults(ff);
            if (!browsing.getResults().isEmpty()) {
                return browsing.getResults().getFirst();
            }
        }
        return null;
    }

    public void register(String pid, List<String> endpoints) {
        pidIssuer.postPID(pid, endpoints);
    }
}
