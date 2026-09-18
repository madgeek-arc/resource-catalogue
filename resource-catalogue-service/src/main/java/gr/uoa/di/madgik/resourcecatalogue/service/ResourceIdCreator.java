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

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationDuplicateCheckProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class ResourceIdCreator implements IdCreator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceIdCreator.class);

    private final SearchService searchService;
    private final CatalogueProperties catalogueProperties;
    private final FederationDuplicateCheckProperties federationProperties;
    private final WebClient federationWebClient;

    private final CircuitBreaker circuitBreaker;


    public ResourceIdCreator(SearchService searchService,
                             CatalogueProperties catalogueProperties,
                             FederationDuplicateCheckProperties federationProperties) {
        this.searchService = searchService;
        this.catalogueProperties = catalogueProperties;
        this.federationProperties = federationProperties;
        this.federationWebClient = WebClient.builder()
                .baseUrl(federationProperties.getSearchUrl())
                .build();
        this.circuitBreaker = new CircuitBreaker(
                federationProperties.getCircuitBreakerFailureThreshold(),
                federationProperties.getCircuitBreakerResetMs());
    }

    @Override
    public String generate(String resourceType) {
        String prefix = createPrefix(resourceType);
        String id = prefix + "/" + randomGenerator();
        if (!prefix.equals("non")) {
            while (searchIdExists(id) || existsInFederation(resourceType, id)) {
                id = prefix + "/" + randomGenerator();
            }
        }
        return id;
    }

    private String createPrefix(String resourceType) {
        try {
            ResourceProperties rp = catalogueProperties.getResourcePropertiesForResourceType(resourceType);
            return rp != null ? rp.getIdPrefix() : "non";
        } catch (IllegalArgumentException e) {
            return "non";
        }
    }

    private String randomGenerator() {
        return RandomStringUtils.randomAlphanumeric(6);
    }

    public boolean searchIdExists(String id) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType("resourceTypes");
        ff.addFilter("resource_internal_id", id);
        Paging<?> resources = searchService.search(ff);
        return resources.getTotal() > 0;
    }

    /**
     * Checks whether the given id is already registered by another node in the federation, via
     * the federated search aggregator's per-resource "fetch by id" endpoint. Fails open: if the
     * check is disabled, not configured for this resource type, or the aggregator is unreachable
     * or the circuit breaker is open, this returns {@code false} (i.e. "not found") rather than
     * blocking id generation on an external dependency.
     */
    boolean existsInFederation(String resourceType, String id) {
        if (!federationProperties.isEnabled()) {
            return false;
        }
        String federationPath = federationPathFor(resourceType);
        if (federationPath == null) {
            return false;
        }
        if (circuitBreaker.isOpen()) {
            logger.debug("Federation duplicate-id check circuit is open; skipping check for id {}", id);
            return false;
        }
        // the aggregator's get-by-id route is /federation/{collection}/{prefix}/{suffix},
        // not a single {id} segment - splitting here avoids the id's own "/" being percent-encoded
        // into the wrong route.
        String[] prefixAndSuffix = id.split("/", 2);
        if (prefixAndSuffix.length != 2) {
            return false;
        }
        try {
            federationWebClient.get()
                    .uri("/{path}/{prefix}/{suffix}", federationPath, prefixAndSuffix[0], prefixAndSuffix[1])
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(federationProperties.getTimeoutMs()))
                    .block();
            circuitBreaker.onSuccess();
            return true;
        } catch (WebClientResponseException.NotFound e) {
            circuitBreaker.onSuccess();
            return false;
        } catch (Exception e) {
            circuitBreaker.onFailure("duplicate-id check for id " + id, e);
            return false;
        }
    }

    private String federationPathFor(String resourceType) {
        try {
            ResourceProperties rp = catalogueProperties.getResourcePropertiesForResourceType(resourceType);
            return rp != null ? rp.getFederationPath() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String sanitizeString(String input) {
        return StringUtils
                .stripAccents(input)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-_/]+", "")
                .replaceAll("[/\\s]+", "_")
                .toLowerCase();
    }

    @Override
    public void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("ID cannot be null or empty");
        }
        if (id.length() > 255) {
            throw new ValidationException("ID is too long; max 255 characters allowed.");
        }
        if (id.chars().anyMatch(c -> c < 0x21 || c > 0x7E)) {
            throw new ValidationException("ID must contain only printable ASCII characters.");
        }
    }
}
