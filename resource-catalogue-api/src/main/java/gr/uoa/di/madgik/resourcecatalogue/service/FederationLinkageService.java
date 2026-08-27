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

import gr.uoa.di.madgik.resourcecatalogue.dto.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only access to resources published on <em>other</em> federation nodes, via the
 * EOSC-Beyond federated search aggregator, so that this node's relational form fields and the
 * Configuration Template / Configuration Template Instance onboarding flow can reference
 * resources anywhere in the federation.
 * <p>
 * Every method degrades gracefully (empty / local-only result) when cross-linkage is disabled
 * or the aggregator is unreachable - callers fall back to local data.
 */
public interface FederationLinkageService {

    /**
     * Merges a resource type's local {@code /list} (id + name pairs for form dropdowns) with the
     * same resource type as published across the federation. The local node's own federated
     * copies are filtered out, so a locally-held resource stays keyed by its low-level id and
     * only genuinely remote resources are added (keyed by their bare PID).
     *
     * @param resourceDisplayName controller-level display name, e.g. "Interoperability Record"
     * @param localResources      the local {@code /list} result
     * @param federation          when false, {@code localResources} is returned unchanged
     */
    List<Value> listResources(String resourceDisplayName, List<Value> localResources, boolean federation);

    /**
     * Fetches a resource by id from whichever federation node owns it.
     *
     * @param resourceDisplayName controller-level display name, e.g. "Service"
     * @param id                  the resource's bare PID ({@code prefix/suffix})
     */
    Optional<Map<String, Object>> getFederatedResource(String resourceDisplayName, String id);

    /**
     * Fetches an Interoperability Record by id from whichever federation node owns it.
     */
    Optional<Map<String, Object>> getInteroperabilityRecord(String id);

    /**
     * Fetches all Configuration Templates of the given Interoperability Record from whichever
     * federation node owns it.
     */
    List<Map<String, Object>> getConfigurationTemplatesByInteroperabilityRecordId(String interoperabilityRecordId);

    /**
     * Fetches the dynamic-form Model bound to the given Configuration Template, from whichever
     * federation node owns the template.
     */
    Optional<Map<String, Object>> getConfigurationTemplateModel(String configurationTemplateId);

    /**
     * Fetches the Configuration Template Instance form/template for a resource + template pair
     * from whichever federation node owns them.
     */
    Optional<Map<String, Object>> getConfigurationTemplateInstanceTemplate(String resourceId,
                                                                          String configurationTemplateId);

    /**
     * Whether federation cross-linkage is enabled on this node at all.
     */
    boolean isEnabled();
}
