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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicDatasourceManager")
public class PublicDatasourceService
        extends AbstractPublicResourceManager<DatasourceBundle>
        implements PublicResourceService<DatasourceBundle> {

    private final ServiceBundleService serviceBundleService;

    public PublicDatasourceService(JmsService jmsService,
                                   PidIssuer pidIssuer,
                                   ServiceBundleService serviceBundleService,
                                   FacetLabelService facetLabelService) {
        super(DatasourceBundle.class, jmsService, pidIssuer, facetLabelService);
        this.serviceBundleService = serviceBundleService;
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }

    @Override
    public void updateIdsToPublic(DatasourceBundle bundle) {
        // serviceId
        ServiceBundle serviceBundle = serviceBundleService.get(
                bundle.getDatasource().getServiceId(), bundle.getDatasource().getCatalogueId(), false);
        bundle.getDatasource().setServiceId(serviceBundle.getIdentifiers().getPid());
    }
}
