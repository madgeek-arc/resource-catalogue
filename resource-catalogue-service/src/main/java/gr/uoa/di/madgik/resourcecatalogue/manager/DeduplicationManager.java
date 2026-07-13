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
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.DuplicatePair;
import gr.uoa.di.madgik.resourcecatalogue.service.DeduplicationService;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationSimilarityClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DeduplicationManager implements DeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicationManager.class);

    private static final int MAX_RESOURCES_PER_SCAN = 10_000;

    private static final int NEIGHBORS_PER_RESOURCE = 20;

    private final GenericResourceService genericResourceService;
    private final FederationSimilarityClient federationSimilarityClient;

    public DeduplicationManager(GenericResourceService genericResourceService,
                                 FederationSimilarityClient federationSimilarityClient) {
        this.genericResourceService = genericResourceService;
        this.federationSimilarityClient = federationSimilarityClient;
    }

    @Override
    public List<DuplicatePair> findDuplicates(String resourceType, Float threshold) {
        float effectiveThreshold = threshold != null ? threshold : 0.95f;
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
                        publishedFilter(resourceType, NEIGHBORS_PER_RESOURCE), sourceId);
                for (Object candidate : similar) {
                    if (!(candidate instanceof ScoredResult<?> sr && sr.getResult() instanceof Bundle b
                            && sr.getScore() >= effectiveThreshold)) {
                        continue;
                    }
                    String candidateId = b.getId();
                    String key = sourceId.compareTo(candidateId) <= 0
                            ? sourceId + "|" + candidateId
                            : candidateId + "|" + sourceId;
                    if (seen.add(key)) {
                        pairs.add(new DuplicatePair(resourceType, sourceId, candidateId, sr.getScore()));
                    }
                }
            } catch (MissingResourceEmbeddingsException | ResourceNotFoundException | ServiceException e) {
                logger.debug("Skipping resource '{}' — no embeddings available", sourceId);
            }
        }
        return pairs;
    }

    @Override
    public List<ScoredResult<LinkedHashMap<String, Object>>> findSimilar(String resourceType, String id,
                                                                          Float threshold, int quantity) {
        float effectiveThreshold = threshold != null ? threshold : 0.95f;
        List<?> results;
        try {
            results = genericResourceService.recommend(publishedFilter(resourceType, quantity), id);
        } catch (MissingResourceEmbeddingsException | ResourceNotFoundException | ServiceException e) {
            logger.debug("No embeddings available for resourceType '{}' — skipping similarity check", resourceType);
            return Collections.emptyList();
        }
        return results.stream()
                .flatMap(obj -> {
                    if (!(obj instanceof ScoredResult<?> sr && sr.getResult() instanceof Bundle b
                            && sr.getScore() >= effectiveThreshold)) {
                        return Stream.empty();
                    }
                    return Stream.of(ScoredResult.of(sr.getScore(), scrubSensitiveFields(b.getPayload(), true)));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoredResult<LinkedHashMap<String, Object>>> findSimilar(String resourceType, Map<String, Object> resource,
                                                                          Float threshold, int quantity) {
        float effectiveThreshold = threshold != null ? threshold : 0.95f;
        FacetFilter ff = publishedFilter(resourceType, quantity);
        List<?> results;
        try {
            // Wrap the candidate in the actual Bundle subclass.
            results = genericResourceService.recommend(ff, wrapResource(resourceType, resource));
        } catch (MissingResourceEmbeddingsException | ResourceNotFoundException | ServiceException e) {
            logger.debug("No embeddings available for resourceType '{}' — skipping local similarity check", resourceType);
            results = Collections.emptyList();
        }
        Stream<ScoredResult<LinkedHashMap<String, Object>>> localMatches = results.stream()
                .flatMap(obj -> {
                    if (!(obj instanceof ScoredResult<?> sr && sr.getResult() instanceof Bundle b
                            && sr.getScore() >= effectiveThreshold)) {
                        return Stream.empty();
                    }
                    return Stream.of(ScoredResult.of(sr.getScore(), scrubSensitiveFields(b.getPayload(), true)));
                });

        // Federation matches are already cosine-similarity scores on the same scale as the local
        // ones (every node runs the same recommend() logic), so they can be merged into one
        // flat, score-sorted list rather than kept as a separate section.
        Stream<ScoredResult<LinkedHashMap<String, Object>>> federationMatches =
                federationSimilarityClient.findSimilar(resourceType, resource, threshold, quantity).stream()
                        .filter(sr -> sr.getScore() >= effectiveThreshold);

        return Stream.concat(localMatches, federationMatches)
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(quantity)
                .collect(Collectors.toList());
    }

    private Object wrapResource(String resourceType, Map<String, Object> resource) {
        Class<?> clazz = genericResourceService.getClassFromResourceType(resourceType);
        if (clazz != null && Bundle.class.isAssignableFrom(clazz)) {
            try {
                Bundle bundle = (Bundle) clazz.getDeclaredConstructor().newInstance();
                bundle.setPayload(new LinkedHashMap<>(resource));
                return bundle;
            } catch (ReflectiveOperationException e) {
                throw new ServiceException("Unable to instantiate " + clazz.getName()
                        + " for resourceType '" + resourceType + "'");
            }
        }
        Map<String, Object> wrappedResource = new LinkedHashMap<>();
        wrappedResource.put(resourceType, resource);
        return wrappedResource;
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
