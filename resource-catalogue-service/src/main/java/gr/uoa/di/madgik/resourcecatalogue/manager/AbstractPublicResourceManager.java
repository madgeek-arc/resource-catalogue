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

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPublicResourceManager<T extends Identifiable> extends ResourceManager<T> {

    @Autowired
    private PublicResourceUtils publicResourceUtils;

    @Value("${catalogue.id}")
    private String catalogueId;

    public AbstractPublicResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    protected void updateServiceIdsToPublic(ServiceBundle serviceBundle) {
        // Resource Organisation
        serviceBundle.getService().setResourceOrganisation(publicResourceUtils.createPublicResourceId(
                serviceBundle.getService().getResourceOrganisation(), serviceBundle.getService().getCatalogueId()));

        // Resource Providers
        serviceBundle.getService().setResourceProviders(
                appendCatalogueId(
                        serviceBundle.getService().getResourceProviders(),
                        serviceBundle.getService().getCatalogueId()));

        // Related Resources
        serviceBundle.getService().setRelatedResources(
                appendCatalogueId(
                        serviceBundle.getService().getRelatedResources(),
                        serviceBundle.getService().getCatalogueId()));

        // Required Resources
        serviceBundle.getService().setRequiredResources(
                appendCatalogueId(
                        serviceBundle.getService().getRequiredResources(),
                        serviceBundle.getService().getCatalogueId()));
    }

    protected void updateDatasourceIdsToPublic(DatasourceBundle datasourceBundle) {
        // serviceId
        datasourceBundle.getDatasource().setServiceId(publicResourceUtils.createPublicResourceId(
                datasourceBundle.getDatasource().getServiceId(), datasourceBundle.getDatasource().getCatalogueId()));
    }

    protected void updateHelpdeskIdsToPublic(HelpdeskBundle helpdeskBundle) {
        // serviceId
        helpdeskBundle.getHelpdesk().setServiceId(publicResourceUtils.createPublicResourceId(
                helpdeskBundle.getHelpdesk().getServiceId(), helpdeskBundle.getCatalogueId()));
    }

    protected void updateMonitoringIdsToPublic(MonitoringBundle monitoringBundle) {
        // serviceId
        monitoringBundle.getMonitoring().setServiceId(publicResourceUtils.createPublicResourceId(
                monitoringBundle.getMonitoring().getServiceId(), monitoringBundle.getCatalogueId()));
    }

    protected void updateTrainingResourceIdsToPublic(TrainingResourceBundle trainingResourceBundle) {
        // Resource Organisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(publicResourceUtils.createPublicResourceId(
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));

        // Resource Providers
        trainingResourceBundle.getTrainingResource().setResourceProviders(
                appendCatalogueId(
                        trainingResourceBundle.getTrainingResource().getResourceProviders(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId()));

        // EOSC Related Services
        trainingResourceBundle.getTrainingResource().setEoscRelatedServices(
                appendCatalogueId(
                        trainingResourceBundle.getTrainingResource().getEoscRelatedServices(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId()));
    }

    protected void updateInteroperabilityRecordIdsToPublic(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        // providerId
        interoperabilityRecordBundle.getInteroperabilityRecord().setProviderId(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
    }

    protected void updateResourceInteroperabilityRecordIdsToPublic(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // resourceId
        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setResourceId(publicResourceUtils.createPublicResourceId(
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
        // Interoperability Record IDs
        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setInteroperabilityRecordIds(
                appendCatalogueId(
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds(),
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
    }

    //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
    protected void updateConfigurationTemplateInstanceIdsToPublic(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        // resourceId
        configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setResourceId(publicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId(), catalogueId));
        //TODO: enable if we have public CT
        // configurationTemplateId
//        configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setResourceId(publicResourceUtils.createPublicResourceId(
//                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getConfigurationTemplateId(), catalogueId));
    }

    protected List<String> appendCatalogueId(List<String> items, String catalogueId) {
        Set<String> transformed = new HashSet<>();
        if (items != null && !items.isEmpty()) {
            for (String item : items) {
                if (item != null && !item.isEmpty()) {
                    item = publicResourceUtils.createPublicResourceId(item, catalogueId);
                    transformed.add(item);
                }
            }
        }
        return new ArrayList<>(transformed);
    }
}
