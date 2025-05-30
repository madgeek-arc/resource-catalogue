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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
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
    private final ProviderResourcesCommonMethods commonMethods;

    public PublicDatasourceService(JmsService jmsService,
                                   ProviderResourcesCommonMethods commonMethods) {
        super(DatasourceBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
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
        Identifiers.createOriginalId(datasourceBundle);
        datasourceBundle.setId(PublicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(datasourceBundle.getId(), datasourceBundle.getDatasource().getCatalogueId());

        // sets public ids to providerId, serviceId
        updateIdsToPublic(datasourceBundle);

        datasourceBundle.getMetadata().setPublished(true);
        DatasourceBundle ret;
        logger.info("Datasource '{}' is being published with id '{}'", lowerLevelResourceId, datasourceBundle.getId());
        ret = super.add(datasourceBundle, null);
        jmsService.convertAndSendTopic("datasource.create", datasourceBundle);
        return ret;
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, Authentication authentication) {
        DatasourceBundle published = super.get(PublicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId()));
        DatasourceBundle ret = super.get(PublicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId()));
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
            DatasourceBundle publicDatasourceBundle = get(PublicResourceUtils.createPublicResourceId(
                    datasourceBundle.getDatasource().getId(),
                    datasourceBundle.getDatasource().getCatalogueId()));
            logger.info("Deleting public Datasource with id '{}'", publicDatasourceBundle.getId());
            super.delete(publicDatasourceBundle);
            jmsService.convertAndSendTopic("datasource.delete", publicDatasourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(DatasourceBundle bundle) {
        // serviceId
        bundle.getDatasource().setServiceId(PublicResourceUtils.createPublicResourceId(
                bundle.getDatasource().getServiceId(), bundle.getDatasource().getCatalogueId()));
    }
}
