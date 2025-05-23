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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ResourceIdCreator implements IdCreator {

    private final SearchService searchService;
    private final Map<ResourceTypes, ResourceProperties> resourceProperties;


    public ResourceIdCreator(SearchService searchService,
                             CatalogueProperties catalogueProperties) {
        this.searchService = searchService;
        this.resourceProperties = catalogueProperties.getResources();
    }

    @Override
    public String generate(String resourceType) {
        String prefix = createPrefix(resourceType);
        String id = prefix + "/" + randomGenerator();
        if (!prefix.equals("non")) {
            while (searchIdExists(id)) {
                id = prefix + "/" + randomGenerator();
            }
        }
        return id;
    }

    private String createPrefix(String resourceType) {
        try {
            return resourceProperties.get(ResourceTypes.valueOf(resourceType.toUpperCase())).getIdPrefix();
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
        if (id == null || id.isEmpty()) {
            throw new ValidationException("ID cannot be null or empty");
        }
        if (id.length() > 50) {
            throw new ValidationException("ID is too long; max 50 characters allowed.");
        }
        if (!id.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ValidationException("Invalid ID: only letters, digits, hyphens, and underscores are allowed.");
        }
    }
}
