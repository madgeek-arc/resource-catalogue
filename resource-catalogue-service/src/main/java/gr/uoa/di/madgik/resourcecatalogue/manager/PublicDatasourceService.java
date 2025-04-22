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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicDatasourceManager")
public class PublicDatasourceService extends ResourceManager<DatasourceBundle>
        implements PublicResourceService<DatasourceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicDatasourceService.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;

    public PublicDatasourceService(JmsService jmsService,
                                   PidIssuer pidIssuer,
                                   ServiceBundleService<ServiceBundle> serviceBundleService) {
        super(DatasourceBundle.class);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.serviceBundleService = serviceBundleService;
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }

    @Override
    public Browsing<DatasourceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    public DatasourceBundle getOrElseReturnNull(String id) {
        DatasourceBundle datasourceBundle;
        try {
            datasourceBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return datasourceBundle;
    }

    @Override
    public DatasourceBundle add(DatasourceBundle datasourceBundle, Authentication authentication) {
        String lowerLevelResourceId = datasourceBundle.getId();
        datasourceBundle.setId(datasourceBundle.getIdentifiers().getPid());
        datasourceBundle.getMetadata().setPublished(true);

        // sets public ids to providerId, serviceId
        updateIdsToPublic(datasourceBundle);

        DatasourceBundle ret;
        logger.info("Datasource '{}' is being published with id '{}'", lowerLevelResourceId, datasourceBundle.getId());
        ret = super.add(datasourceBundle, null);
        jmsService.convertAndSendTopic("datasource.create", datasourceBundle);
        return ret;
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, Authentication authentication) {
        DatasourceBundle published = super.get(datasourceBundle.getIdentifiers().getPid());
        DatasourceBundle ret = super.get(datasourceBundle.getIdentifiers().getPid());
        try {
            BeanUtils.copyProperties(ret, datasourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to providerId, serviceId
        updateIdsToPublic(datasourceBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Datasource with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("datasource.update", ret);
        return ret;
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        try {
            DatasourceBundle publicDatasourceBundle = get(datasourceBundle.getIdentifiers().getPid());
            logger.info("Deleting public Datasource with id '{}'", publicDatasourceBundle.getId());
            super.delete(publicDatasourceBundle);
            jmsService.convertAndSendTopic("datasource.delete", publicDatasourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(DatasourceBundle bundle) {
        // serviceId
        ServiceBundle serviceBundle = serviceBundleService.get(
                bundle.getDatasource().getServiceId(), bundle.getDatasource().getCatalogueId());
        bundle.getDatasource().setServiceId(serviceBundle.getIdentifiers().getPid());
    }
}
