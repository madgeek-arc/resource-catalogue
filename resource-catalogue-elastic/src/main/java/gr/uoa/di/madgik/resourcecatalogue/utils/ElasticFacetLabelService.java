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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import gr.uoa.di.madgik.registry.domain.Facet;
import co.elastic.clients.json.JsonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ElasticFacetLabelService implements FacetLabelService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticFacetLabelService.class);

    private final ElasticsearchClient client;

    ElasticFacetLabelService(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public List<Facet> generateLabels(List<Facet> facets) {
        Map<String, String> vocabularyValues = null;
        try {
            vocabularyValues = getIdNameFields();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        for (Facet facet : facets) {
            Map<String, String> finalVocabularyValues = vocabularyValues;
            facet.getValues().forEach(value -> value.setLabel(getLabelElseKeepValue(value.getValue(), finalVocabularyValues)));
        }
        return facets;
    }

    private Map<String, String> getIdNameFields() throws IOException {
        Map<String, String> idNameMap = new TreeMap<>();
        String scrollId = null;

        try {
            SearchResponse<Void> searchResponse = client.search(s -> s
                            .index("resourceTypes")
                            .scroll(sc -> sc.time("1m"))
                            .size(10000)
                            .source(src -> src.fetch(false))
                            .fields(
                                    FieldAndFormat.of(f -> f.field("resource_internal_id")),
                                    FieldAndFormat.of(f -> f.field("name")),
                                    FieldAndFormat.of(f -> f.field("title"))),
                    Void.class);

            scrollId = searchResponse.scrollId();
            processHits(searchResponse.hits().hits(), idNameMap);

            while (hasHits(searchResponse.hits().hits()) && scrollId != null && !scrollId.isBlank()) {
                String currentScrollId = scrollId;
                ScrollResponse<Void> scrollResponse = client.scroll(s -> s
                                .scrollId(currentScrollId)
                                .scroll(sc -> sc.time("1m")),
                        Void.class);
                scrollId = scrollResponse.scrollId();
                processHits(scrollResponse.hits().hits(), idNameMap);
                if (!hasHits(scrollResponse.hits().hits())) {
                    break;
                }
            }
        } finally {
            clearScroll(scrollId);
        }

        return idNameMap;
    }

    private void processHits(List<Hit<Void>> hits, Map<String, String> idNameMap) {
        for (Hit<Void> hit : hits) {
            Map<String, JsonData> fields = hit.fields();
            String id = firstFieldValue(fields.get("resource_internal_id"));
            if (id == null) {
                logger.error("Could not create id - name value. Hit: {}", hit);
                continue;
            }
            String name = firstFieldValue(fields.get("name"));
            String title = firstFieldValue(fields.get("title"));
            if (name != null) {
                idNameMap.put(id, name);
            } else if (title != null) {
                idNameMap.put(id, title);
            }
        }
    }

    private boolean hasHits(List<Hit<Void>> hits) {
        return hits != null && !hits.isEmpty();
    }

    private String firstFieldValue(JsonData field) {
        if (field == null) {
            return null;
        }
        Object value = field.to(Object.class);
        if (value instanceof List<?> values && !values.isEmpty()) {
            Object first = values.getFirst();
            return first != null ? first.toString() : null;
        }
        return value != null ? value.toString() : null;
    }

    private void clearScroll(String scrollId) {
        if (scrollId == null || scrollId.isBlank()) {
            return;
        }
        try {
            client.clearScroll(c -> c.scrollId(scrollId));
        } catch (IOException e) {
            logger.error("clear scroll request failed...", e);
        }
    }

    String toProperCase(String str, String delimiter, String newDelimiter) {
        if (str.equals("")) {
            str = "-";
        }
        StringJoiner joiner = new StringJoiner(newDelimiter);
        for (String s : str.split(delimiter)) {
            try {
                String s1;
                s1 = s.substring(0, 1).toUpperCase() + s.substring(1);
                joiner.add(s1);
            } catch (IndexOutOfBoundsException e) {
                return str;
            }
        }
        return joiner.toString();
    }

    String getLabelElseKeepValue(String value, Map<String, String> labels) {
        if (labels == null) {
            return toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        String ret = labels.get(value);
        if (ret == null) {
            ret = toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        return ret;
    }
}
