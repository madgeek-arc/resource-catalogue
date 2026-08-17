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
        Bundle source = resolveSource(resourceType, id);
        // The public copy of `id` is indexed under its own PID as resource_internal_id, so the
        // registry's own-id exclusion (which matches on the id we search with) can't filter it
        // out. Drop it from the local results below instead.
        String ownPublicId = source != null && source.getIdentifiers() != null
                ? source.getIdentifiers().getPid() : null;

        List<?> results;
        try {
            results = genericResourceService.recommend(publishedFilter(resourceType, quantity + 1), id);
        } catch (MissingResourceEmbeddingsException | ResourceNotFoundException | ServiceException e) {
            logger.debug("No embeddings available for resourceType '{}' — skipping local similarity check", resourceType);
            results = Collections.emptyList();
        }
        List<ScoredResult<LinkedHashMap<String, Object>>> localMatches = results.stream()
                .flatMap(obj -> {
                    if (!(obj instanceof ScoredResult<?> sr && sr.getResult() instanceof Bundle b
                            && sr.getScore() >= effectiveThreshold)) {
                        return Stream.empty();
                    }
                    if (b.getId().equals(ownPublicId)) {
                        return Stream.empty();
                    }
                    return Stream.of(ScoredResult.of(sr.getScore(), scrubSensitiveFields(b.getPayload(), true)));
                })
                .collect(Collectors.toList());

        // Node-specific EPOT notification also gets a federation-wide view: a resource pending
        // review on this node may already duplicate one published on another node, which is
        // exactly the kind of thing a reviewer should know about before approving it. This calls
        // the aggregator's /similar route, which fans out to every node's local-only
        // /dedup/{resourceType}/check/local — never back into this method — so it cannot recurse.
        List<ScoredResult<LinkedHashMap<String, Object>>> federationMatches = source == null
                ? Collections.emptyList()
                : federationSimilarityClient.findSimilar(resourceType, source.getPayload(), threshold, quantity).stream()
                        .filter(sr -> sr.getScore() >= effectiveThreshold)
                        .collect(Collectors.toList());

        return mergeLocalAndFederation(localMatches, federationMatches, quantity);
    }

    private Bundle resolveSource(String resourceType, String id) {
        try {
            return genericResourceService.get(resourceType, id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    @Override
    public List<ScoredResult<LinkedHashMap<String, Object>>> findSimilar(String resourceType, Map<String, Object> resource,
                                                                          Float threshold, int quantity) {
        float effectiveThreshold = threshold != null ? threshold : 0.95f;
        List<ScoredResult<LinkedHashMap<String, Object>>> localMatches =
                findSimilarLocally(resourceType, resource, threshold, quantity);

        // Federation matches are already cosine-similarity scores on the same scale as the local
        // ones (every node runs the same recommend() logic), so they can be merged into one
        // flat, score-sorted list rather than kept as a separate section. The aggregator fans this
        // out to every node's local-only /check/local, including this one, so our own matches can
        // legitimately reappear here too — mergeLocalAndFederation() drops that echo.
        List<ScoredResult<LinkedHashMap<String, Object>>> federationMatches =
                federationSimilarityClient.findSimilar(resourceType, resource, threshold, quantity).stream()
                        .filter(sr -> sr.getScore() >= effectiveThreshold)
                        .collect(Collectors.toList());

        return mergeLocalAndFederation(localMatches, federationMatches, quantity);
    }

    @Override
    public List<ScoredResult<LinkedHashMap<String, Object>>> findSimilarLocally(String resourceType, Map<String, Object> resource,
                                                                                  Float threshold, int quantity) {
        float effectiveThreshold = threshold != null ? threshold : 0.95f;
        FacetFilter ff = publishedFilter(resourceType, quantity);
        List<?> results;
        try {
            // Wrap the candidate in the actual Bundle subclass.
            results = genericResourceService.recommend(ff, wrapResource(resourceType, resource));
        } catch (MissingResourceEmbeddingsException | ResourceNotFoundException | ServiceException e) {
            logger.debug("No embeddings available for resourceType '{}' — skipping local similarity check", resourceType);
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

    /**
     * Merges local and federation matches, preferring the local copy when the same resource
     * (identified by its payload id / PID) appears in both — which happens whenever this node
     * itself is reachable through the federation aggregator's own fan-out.
     */
    private List<ScoredResult<LinkedHashMap<String, Object>>> mergeLocalAndFederation(
            List<ScoredResult<LinkedHashMap<String, Object>>> localMatches,
            List<ScoredResult<LinkedHashMap<String, Object>>> federationMatches,
            int quantity) {
        Set<Object> seenIds = new HashSet<>();
        List<ScoredResult<LinkedHashMap<String, Object>>> merged =
                new ArrayList<>(localMatches.size() + federationMatches.size());
        for (ScoredResult<LinkedHashMap<String, Object>> sr : localMatches) {
            if (seenIds.add(sr.getResult().get("id"))) {
                merged.add(sr);
            }
        }
        for (ScoredResult<LinkedHashMap<String, Object>> sr : federationMatches) {
            if (seenIds.add(sr.getResult().get("id"))) {
                merged.add(sr);
            }
        }
        return merged.stream()
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
