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
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicMonitoringManager")
public class PublicMonitoringService extends ResourceManager<MonitoringBundle>
        implements PublicResourceService<MonitoringBundle> {

    private static final Logger logger = LoggerFactory.getLogger(PublicMonitoringService.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;

    public PublicMonitoringService(JmsService jmsService,
                                   ProviderResourcesCommonMethods commonMethods) {
        super(MonitoringBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceTypeName() {
        return "monitoring";
    }

    @Override
    public Browsing<MonitoringBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    public MonitoringBundle getOrElseReturnNull(String id) {
        MonitoringBundle monitoringBundle;
        try {
            monitoringBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return monitoringBundle;
    }

    @Override
    public MonitoringBundle add(MonitoringBundle monitoringBundle, Authentication authentication) {
        String lowerLevelResourceId = monitoringBundle.getId();
        Identifiers.createOriginalId(monitoringBundle);
        monitoringBundle.setId(PublicResourceUtils.createPublicResourceId(monitoringBundle.getMonitoring().getId(),
                monitoringBundle.getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(monitoringBundle.getId(), monitoringBundle.getCatalogueId());

        // sets public id to serviceId
        updateIdsToPublic(monitoringBundle);

        monitoringBundle.getMetadata().setPublished(true);
        MonitoringBundle ret;
        logger.info("Monitoring '{}' is being published with id '{}'", lowerLevelResourceId, monitoringBundle.getId());
        ret = super.add(monitoringBundle, null);
        jmsService.convertAndSendTopic("monitoring.create", monitoringBundle);
        return ret;
    }

    @Override
    public MonitoringBundle update(MonitoringBundle monitoringBundle, Authentication authentication) {
        MonitoringBundle published = super.get(PublicResourceUtils.createPublicResourceId(monitoringBundle.getMonitoring().getId(),
                monitoringBundle.getCatalogueId()));
        MonitoringBundle ret = super.get(PublicResourceUtils.createPublicResourceId(monitoringBundle.getMonitoring().getId(),
                monitoringBundle.getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, monitoringBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public id to serviceId
        updateIdsToPublic(monitoringBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Monitoring with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("monitoring.update", monitoringBundle);
        return ret;
    }

    @Override
    public void delete(MonitoringBundle monitoringBundle) {
        try {
            MonitoringBundle publicMonitoringBundle = get(PublicResourceUtils.createPublicResourceId(
                    monitoringBundle.getMonitoring().getId(),
                    monitoringBundle.getCatalogueId()));
            logger.info("Deleting public Monitoring with id '{}'", publicMonitoringBundle.getId());
            super.delete(publicMonitoringBundle);
            jmsService.convertAndSendTopic("monitoring.delete", publicMonitoringBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Override
    public void updateIdsToPublic(MonitoringBundle bundle) {
        // serviceId
        bundle.getMonitoring().setServiceId(PublicResourceUtils.createPublicResourceId(
                bundle.getMonitoring().getServiceId(), bundle.getCatalogueId()));
    }
}
