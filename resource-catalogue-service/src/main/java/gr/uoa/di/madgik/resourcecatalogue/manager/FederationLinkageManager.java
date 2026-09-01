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

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationResourceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default {@link FederationLinkageService}: normalises the resource type, resolves its
 * aggregator collection segment from {@link ResourceProperties#getFederationPath()}, and
 * delegates the actual HTTP to {@link FederationResourceClient}.
 * <p>
 * De-duplication rationale (see {@link #localDedupKeys}): the local node is itself reachable
 * through the aggregator, so its own copies come back in the federation slice. A locally-held
 * resource must stay offered as its low-level id (PID + {@code "00"}) so downstream
 * {@code updateIdsToPublic} resolves it normally; only genuinely remote resources are offered
 * by their bare PID and later stored verbatim.
 */
@Service
public class FederationLinkageManager implements FederationLinkageService {

    private static final Logger logger = LoggerFactory.getLogger(FederationLinkageManager.class);

    private static final String LOW_LEVEL_ID_SUFFIX = "00";

    private final CatalogueProperties catalogueProperties;
    private final FederationResourceClient federationResourceClient;

    public FederationLinkageManager(CatalogueProperties catalogueProperties,
                                    FederationResourceClient federationResourceClient) {
        this.catalogueProperties = catalogueProperties;
        this.federationResourceClient = federationResourceClient;
    }

    @Override
    public boolean isEnabled() {
        return federationResourceClient.isEnabled();
    }

    @Override
    public List<Value> listResources(String resourceDisplayName, List<Value> localResources, boolean federation) {
        if (!federation || !federationResourceClient.isEnabled()) {
            return localResources;
        }
        String federationPath = federationPathFor(resourceDisplayName);
        if (federationPath == null || federationPath.isBlank()) {
            return localResources;
        }

        Set<String> localKeys = localDedupKeys(localResources);
        List<Map<String, Object>> federated = federationResourceClient.listAll(federationPath);

        List<Value> merged = new ArrayList<>(localResources);
        Set<String> addedFederatedIds = new HashSet<>();
        for (Map<String, Object> payload : federated) {
            Object idObj = payload.get("id");
            Object nameObj = payload.get("name");
            if (!(idObj instanceof String id) || id.isBlank()) {
                continue;
            }
            if (localKeys.contains(id) || !addedFederatedIds.add(id)) {
                continue;
            }
            merged.add(new Value(id, nameObj != null ? nameObj.toString() : id));
        }
        logger.debug("Federation list for '{}': {} local + {} federated (after de-dup)",
                resourceDisplayName, localResources.size(), merged.size() - localResources.size());
        return merged;
    }

    @Override
    public Optional<Map<String, Object>> getFederatedResource(String resourceDisplayName, String id) {
        String federationPath = federationPathFor(resourceDisplayName);
        String[] ps = splitPid(id);
        if (federationPath == null || federationPath.isBlank() || ps == null) {
            return Optional.empty();
        }
        return federationResourceClient.getById(federationPath, ps[0], ps[1]);
    }

    @Override
    public Boolean federatedResourceExists(String resourceDisplayName, String id) {
        String federationPath = federationPathFor(resourceDisplayName);
        String[] ps = splitPid(id);
        if (federationPath == null || federationPath.isBlank() || ps == null) {
            return Boolean.FALSE;
        }
        return federationResourceClient.existsById(federationPath, ps[0], ps[1]);
    }

    @Override
    public Optional<Map<String, Object>> getInteroperabilityRecord(String id) {
        String[] ps = splitPid(id);
        if (ps == null) {
            return Optional.empty();
        }
        return federationResourceClient.getById("interoperabilityRecords", ps[0], ps[1]);
    }

    @Override
    public List<Map<String, Object>> getConfigurationTemplatesByInteroperabilityRecordId(String interoperabilityRecordId) {
        String[] ps = splitPid(interoperabilityRecordId);
        if (ps == null) {
            return Collections.emptyList();
        }
        return federationResourceClient.getConfigurationTemplatesByInteroperabilityRecordId(ps[0], ps[1]);
    }

    @Override
    public Optional<Map<String, Object>> getConfigurationTemplateModel(String configurationTemplateId) {
        String[] ps = splitPid(configurationTemplateId);
        if (ps == null) {
            return Optional.empty();
        }
        return federationResourceClient.getConfigurationTemplateModel(ps[0], ps[1]);
    }

    @Override
    public Optional<Map<String, Object>> getConfigurationTemplateInstanceTemplate(String resourceId,
                                                                                 String configurationTemplateId) {
        String[] res = splitPid(resourceId);
        String[] ct = splitPid(configurationTemplateId);
        if (res == null || ct == null) {
            return Optional.empty();
        }
        return federationResourceClient.getConfigurationTemplateInstanceTemplate(res[0], res[1], ct[0], ct[1]);
    }

    /**
     * Local ids as-is plus, for every low-level id ({@code <pid>00}), the bare PID it derives
     * from - so a federation entry keyed by that bare PID is recognised as the local copy.
     */
    private Set<String> localDedupKeys(List<Value> localResources) {
        Set<String> keys = new HashSet<>();
        for (Value value : localResources) {
            String id = value.getId();
            if (id == null) {
                continue;
            }
            keys.add(id);
            if (id.endsWith(LOW_LEVEL_ID_SUFFIX) && id.length() > LOW_LEVEL_ID_SUFFIX.length()) {
                keys.add(id.substring(0, id.length() - LOW_LEVEL_ID_SUFFIX.length()));
            }
        }
        return keys;
    }

    private String federationPathFor(String resourceDisplayName) {
        String key = resourceDisplayName.trim().toLowerCase().replace(' ', '_');
        try {
            ResourceProperties rp = catalogueProperties.getResourcePropertiesForResourceType(key);
            return rp != null ? rp.getFederationPath() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Splits a PID ({@code prefix/suffix}) into its two aggregator path segments. Returns
     * {@code null} for anything not PID-shaped.
     */
    private String[] splitPid(String id) {
        if (id == null) {
            return null;
        }
        String[] parts = id.split("/", 2);
        return parts.length == 2 ? parts : null;
    }
}
