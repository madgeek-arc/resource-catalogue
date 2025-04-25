/**
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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceCatalogueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public abstract class ResourceCatalogueManager<T extends Identifiable> extends ResourceManager<T> implements ResourceCatalogueService<T> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCatalogueManager.class);

    @Value("${catalogue.id}")
    private String catalogueId;

    public ResourceCatalogueManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public T add(T t, Authentication auth) {
        String catalogueId = getCatalogueId(t);
        boolean published = getLevel(t);
        if (existsInTheSameCatalogueAndLevel(t, catalogueId, published)) {
            throw new ResourceAlreadyExistsException(String.format("%s with id = '%s', catalogueId = '%s' " +
                    "and published = '%s' already exists!", getResourceTypeName(), t.getId(), catalogueId, published));
        }
        String serialized = serialize(t);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(getResourceType());
        resourceService.addResource(created);
        logger.debug("Adding Resource {}", t);
        return t;
    }

    @Override
    public T update(T t, Authentication auth) {
        String catalogueId = getCatalogueId(t);
        boolean published = getLevel(t);
        Resource existing = whereID(t.getId(), catalogueId, published, true);
        existing.setPayload(serialize(t));
        existing.setResourceType(getResourceType());
        resourceService.updateResource(existing);
        logger.debug("Updating Resource {}", t);
        return t;
    }

    @Override
    public void delete(T t) {
        String catalogueId = getCatalogueId(t);
        boolean published = getLevel(t);
        resourceService.deleteResource(whereID(t.getId(), catalogueId, published, true).getId());
        logger.debug("Deleting Resource {}", t);
    }

    protected String getCatalogueId(T t) {
        if (t instanceof ProviderBundle) {
            return ((ProviderBundle) t).getProvider().getCatalogueId();
        } else if (t instanceof ServiceBundle) {
            return ((ServiceBundle) t).getService().getCatalogueId();
        } else if (t instanceof TrainingResourceBundle) {
            return ((TrainingResourceBundle) t).getTrainingResource().getCatalogueId();
        } else if (t instanceof InteroperabilityRecordBundle) {
            return ((InteroperabilityRecordBundle) t).getInteroperabilityRecord().getCatalogueId();
        } else if (t instanceof ResourceInteroperabilityRecordBundle) {
            return ((ResourceInteroperabilityRecordBundle) t).getResourceInteroperabilityRecord().getCatalogueId();
        } else if (t instanceof DatasourceBundle) {
            return ((DatasourceBundle) t).getDatasource().getCatalogueId();
        } else if (t instanceof MonitoringBundle) {
            return ((MonitoringBundle) t).getCatalogueId();
        } else if (t instanceof HelpdeskBundle) {
            return ((HelpdeskBundle) t).getCatalogueId();
        }
        return catalogueId;
    }

    protected boolean getLevel(T t) {
        if (t instanceof Bundle) {
            return ((Bundle<?>) t).getMetadata().isPublished();
        }
        return false;
    }

    protected boolean existsInTheSameCatalogueAndLevel(T t, String catalogueId, boolean published) {
        return existsInTheSameCatalogueAndLevel(t.getId(), catalogueId, published);
    }

    protected boolean existsInTheSameCatalogueAndLevel(String id, String catalogueId, boolean published) {
        return id != null && whereID(id, catalogueId, published, false) != null;
    }

    protected Resource whereID(String id, String catalogueId, boolean published, boolean throwOnNull) {
        return id == null ? null : where(throwOnNull,
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("catalogue_id", catalogueId),
                new SearchService.KeyValue("published", String.valueOf(published)));
    }

    protected Resource where(boolean throwOnNull, SearchService.KeyValue... keyValues) {
        Resource ret;
        ret = searchService.searchFields(getResourceTypeName(), keyValues);
        if (throwOnNull && ret == null) {
            throw new ResourceException(String.format("%s does not exist!", getResourceTypeName()), HttpStatus.NOT_FOUND);
        }
        return ret;
    }

    protected String serialize(T t) {
        String ret = parserPool.serialize(t, ParserService.ParserServiceTypes.fromString(getResourceType().getPayloadType()));
        if (ret.equals("failed")) {
            throw new ResourceException(String.format("Not a valid %s!", getResourceTypeName()), HttpStatus.BAD_REQUEST);
        }
        return ret;
    }

    public T get(String id, String catalogueId, boolean published) {
        Resource resource = getResource(id, catalogueId, published);
        if (resource == null) {
            throw new CatalogueResourceNotFoundException(String.format("Could not find %s with id: %s and catalogueId: %s",
                    typeParameterClass.getSimpleName(), id, catalogueId));
        }
        return deserialize(resource);
    }

    public Resource getResource(String id, String catalogueId, boolean published) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", published);
        ff.setResourceType(getResourceTypeName());
        return searchService.searchFields(
                getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("catalogue_id", catalogueId),
                new SearchService.KeyValue("published", String.valueOf(published))
        );
    }
}
