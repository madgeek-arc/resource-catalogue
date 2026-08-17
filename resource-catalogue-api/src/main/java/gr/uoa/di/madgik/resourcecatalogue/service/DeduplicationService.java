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

import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.resourcecatalogue.dto.DuplicatePair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface DeduplicationService {

    List<DuplicatePair> findDuplicates(String resourceType, Float threshold);

    List<ScoredResult<LinkedHashMap<String, Object>>> findSimilar(String resourceType, String id, Float threshold, int quantity);

    List<ScoredResult<LinkedHashMap<String, Object>>> findSimilar(String resourceType, Map<String, Object> resource, Float threshold, int quantity);

    /**
     * Local-node-only equivalent of {@link #findSimilar(String, Map, Float, int)} — no federation
     * fan-out. This is what the federation aggregator itself calls on each node when it fans a
     * check out across the federation; it must never trigger a further federation call, or a node
     * calling the aggregator would cause the aggregator to call back into that same node, which
     * would call the aggregator again, recursing without bound.
     */
    List<ScoredResult<LinkedHashMap<String, Object>>> findSimilarLocally(String resourceType, Map<String, Object> resource, Float threshold, int quantity);
}
