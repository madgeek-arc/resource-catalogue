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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.exception.MissingResourceEmbeddingsException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.DuplicatePair;
import gr.uoa.di.madgik.resourcecatalogue.service.DeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeduplicationManager implements DeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicationManager.class);

    private static final int MAX_RESOURCES_PER_SCAN = 10_000;

    private final GenericResourceService genericResourceService;

    public DeduplicationManager(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @Override
    public List<DuplicatePair> findDuplicates(String resourceType, int quantity) {
        Paging<?> all = genericResourceService.getResults(publishedFilter(resourceType, MAX_RESOURCES_PER_SCAN));

        Set<String> seen = new LinkedHashSet<>();
        List<DuplicatePair> pairs = new ArrayList<>();

        for (Object obj : all.getResults()) {
            if (!(obj instanceof Bundle source)) {
                continue;
            }
            String sourceId = source.getId();
            try {
                List<?> similar = genericResourceService.recommend(
                        publishedFilter(resourceType, quantity), sourceId);
                for (Object candidate : similar) {
                    if (!(candidate instanceof ScoredResult<?> sr && sr.getResult() instanceof Bundle b)) {
                        continue;
                    }
                    String candidateId = b.getId();
                    String key = sourceId.compareTo(candidateId) <= 0
                            ? sourceId + "|" + candidateId
                            : candidateId + "|" + sourceId;
                    if (seen.add(key)) {
                        pairs.add(new DuplicatePair(resourceType, sourceId, candidateId));
                    }
                }
            } catch (MissingResourceEmbeddingsException e) {
                logger.debug("Skipping resource '{}' — no embeddings available", sourceId);
            }
        }
        return pairs;
    }

    @Override
    public List<LinkedHashMap<String, Object>> findSimilar(String resourceType, String id, int quantity) {
        return genericResourceService.recommend(publishedFilter(resourceType, quantity), id).stream()
                .filter(obj -> obj instanceof ScoredResult<?> sr && sr.getResult() instanceof Bundle)
                .map(obj -> scrubSensitiveFields(((Bundle) ((ScoredResult<?>) obj).getResult()).getPayload(), true))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> scrubSensitiveFields(Map<?, ?> source, boolean isRoot) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = (String) entry.getKey();
            if (isRoot && "users".equals(key)) {
                continue;
            }
            if ("email".equals(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(key, scrubSensitiveFields(nested, false));
            } else if (value instanceof List<?> list) {
                copy.put(key, scrubList(list));
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private List<Object> scrubList(List<?> list) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> nested) {
                result.add(scrubSensitiveFields(nested, false));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private FacetFilter publishedFilter(String resourceType, int quantity) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType);
        ff.addFilter("published", true);
        ff.addFilter("draft", false);
        ff.setQuantity(quantity);
        return ff;
    }
}
