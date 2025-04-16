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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface OpenAIREDatasourceService {

    /**
     * Get a specific external Datasource given its ID.
     *
     * @param id Datasource ID
     * @return {@link Datasource}
     * @throws IOException If an input or output exception occurred
     */
    Datasource get(String id) throws IOException;

    /**
     * Get a list of all Datasources.
     *
     * @param ff FacetFilter
     * @return {@link Map <>}
     * @throws IOException If an input or output exception occurred
     */
    Map<Integer, List<Datasource>> getAll(FacetFilter ff) throws IOException;

    /**
     * Returns various metrics of an OpenAIRE Datasource, given the EOSC Datasource ID
     *
     * @param id Datasource ID
     * @return {@link Boolean}
     */
    OpenAIREMetrics getMetrics(String id);
}
