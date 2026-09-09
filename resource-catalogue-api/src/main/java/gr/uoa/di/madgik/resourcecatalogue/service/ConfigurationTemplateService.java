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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

public interface ConfigurationTemplateService extends ResourceCatalogueGenericService<ConfigurationTemplateBundle> {

    /**
     * Return all Configuration Templates under a specific Interoperability Record ID, from the
     * private layer. The link is keyed by the Interoperability Record's local id - the only id
     * form a node deals in for its own CUD operations.
     *
     * @param params                   search parameters
     * @param interoperabilityRecordId Interoperability Record local id
     * @return {@link Paging<ConfigurationTemplateBundle>}
     */
    Paging<ConfigurationTemplateBundle> getAllByInteroperabilityRecordId(MultiValueMap<String, Object> params,
                                                                         String interoperabilityRecordId);

    /**
     * Public-layer counterpart of {@link #getAllByInteroperabilityRecordId(MultiValueMap, String)}:
     * returns the published &amp; active Configuration Templates of the Interoperability Record
     * identified by its <em>public</em> PID - the id form the public layer stores after id
     * translation on publish. Cross-node federation reads only ever see public layers, so the
     * federated-search aggregator calls this (through
     * {@code public/configurationTemplate/getAllByInteroperabilityRecordId}).
     *
     * @param params                   search parameters
     * @param interoperabilityRecordId Interoperability Record public PID
     * @return {@link Paging<ConfigurationTemplateBundle>}
     */
    Paging<ConfigurationTemplateBundle> getPublicByInteroperabilityRecordId(MultiValueMap<String, Object> params,
                                                                            String interoperabilityRecordId);

    /**
     * Return a mapping of Interoperability Record ID to Configuration Template list.
     *
     * @return {@link Map}
     */
    Map<String, List<String>> getInteroperabilityRecordIdToConfigurationTemplateListMap();


    /**
     * Returns all the available Service Types
     *
     * @return {@link List<Vocabulary>}
     */
    List<Vocabulary> getAvailableServiceTypes();
}