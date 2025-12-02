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

import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

@Service("publicConfigurationTemplateManager")
public class PublicConfigurationTemplateService
        extends AbstractPublicResourceManager<ConfigurationTemplateBundle>
        implements PublicResourceService<ConfigurationTemplateBundle> {

    private final InteroperabilityRecordService interoperabilityRecordService;

    public PublicConfigurationTemplateService(JmsService jmsService,
                                              PidIssuer pidIssuer,
                                              FacetLabelService facetLabelService,
                                              InteroperabilityRecordService interoperabilityRecordService) {
        super(ConfigurationTemplateBundle.class, jmsService, pidIssuer, facetLabelService);
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template";
    }

    public void updateIdsToPublic(ConfigurationTemplateBundle bundle) {
        // resourceId
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(
                bundle.getConfigurationTemplate().getInteroperabilityRecordId(), bundle.getConfigurationTemplate().getCatalogueId(), false);
        bundle.getConfigurationTemplate().setInteroperabilityRecordId(interoperabilityRecordBundle.getIdentifiers().getPid());
    }

}
