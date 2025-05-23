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

package gr.uoa.di.madgik.resourcecatalogue.service.search;

import gr.uoa.di.madgik.registry.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.util.List;
import java.util.Map;

//@Service
public class SearchServiceEIC extends AbstractSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceEIC.class);

    public SearchServiceEIC(RestHighLevelClient elasticsearchClient) {
        super(elasticsearchClient);
    }

    @Override
    public BoolQueryBuilder customFilters(BoolQueryBuilder qBuilder, Map<String, List<Object>> allFilters) {
        for (Map.Entry<String, List<Object>> filters : allFilters.entrySet()) {

            switch (filters.getKey()) {
                case "active":
                    qBuilder.filter(createDisMaxQuery(filters.getKey(), filters.getValue()));
                    break;

                default:
                    qBuilder.must(createDisMaxQuery(filters.getKey(), filters.getValue()));
                    break;
            }
        }
        return qBuilder;
    }
}
