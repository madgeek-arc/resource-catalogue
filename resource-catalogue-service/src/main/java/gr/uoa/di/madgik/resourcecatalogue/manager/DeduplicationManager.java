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
import gr.uoa.di.madgik.registry.exception.MissingResourceEmbeddingsException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.DuplicatePair;
import gr.uoa.di.madgik.resourcecatalogue.service.DeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DeduplicationManager implements DeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicationManager.class);

    private static final int MAX_RESOURCES_PER_SCAN = 10_000;

    @Value("${dedup.candidates-per-resource:5}")
    private int candidatesPerResource;

    private final GenericResourceService genericResourceService;

    public DeduplicationManager(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @Override
    public List<DuplicatePair> findDuplicates(String resourceType) {
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
                        publishedFilter(resourceType, candidatesPerResource), sourceId);
                for (Object candidate : similar) {
                    if (!(candidate instanceof Bundle b)) {
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
    public List<?> findSimilar(String resourceType, String id) {
        return genericResourceService.recommend(publishedFilter(resourceType, candidatesPerResource), id);
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
