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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

public abstract class ResourceManager<T extends Identifiable> implements ResourceService<T> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    protected final GenericResourceService genericResourceService;
    protected final IdCreator idCreator;
    protected final int maxQuantity;

    public ResourceManager(GenericResourceService genericResourceService, IdCreator idCreator, int maxQuantity) {
        this.genericResourceService = genericResourceService;
        this.idCreator = idCreator;
        this.maxQuantity = maxQuantity;
    }

    public abstract String getResourceTypeName();

    @Override
    public String createId(T t) {
        return idCreator.generate(getResourceTypeName());
    }

    @Override
    public T get(String id) {
        return genericResourceService.get(getResourceTypeName(), id);
    }

    @Override
    public Resource getResource(String id) {
        return genericResourceService.searchResource(getResourceTypeName(), id, true);
    }

    @Override
    public Paging<T> getAll(FacetFilter filter) {
        if (filter.getResourceType() == null) {
            filter.setResourceType(getResourceTypeName());
        }
        return genericResourceService.getResults(filter);
    }

    @Override
    public Paging<T> getAll(FacetFilter ff, Authentication auth) {
        if (ff.getResourceType() == null) {
            ff.setResourceType(getResourceTypeName());
        }
        return genericResourceService.getResults(ff);
    }

    @Override
    public Paging<T> getMy(FacetFilter ff, Authentication auth) {
        return null;
    }

    @Override
    public T add(T t, Authentication auth) {
        logger.debug("Adding Resource {}", t);
        t = genericResourceService.add(getResourceTypeName(), t);
        return t;
    }

    @Override
    public T update(T t, Authentication auth) {
        logger.debug("Updating Resource {}", t);
        t = genericResourceService.update(getResourceTypeName(), t);
        return t;
    }

    @Override
    public final T save(T t) {
        Resource resource = new Resource();
        if (exists(t)) { // update
            logger.debug("Updated Resource: {}", t);
            t = this.update(t, null);
        } else { // add
            // create id
            String id = createId(t);
            t.setId(id);
            // save
            logger.debug("Added Resource: {}", t);
            t = this.add(t, null);
        }
        return t;
    }

    @Override
    public void delete(T t) {
        logger.debug("Deleting Resource {}", t);
        genericResourceService.delete(getResourceTypeName(), t.getId());
    }

    @Override
    public T get(SearchService.KeyValue... keyValues) {
        return genericResourceService.get(getResourceTypeName(), keyValues);
    }

    @Override
    public T validate(T t) {
        logger.debug("Validating Resource {} using FieldValidator", t);
        genericResourceService.validate(getResourceTypeName(), t);
        return t;
    }

    @Override
    public boolean exists(T t) {
        return genericResourceService.exists(getResourceTypeName(), t);
    }

    @Override
    public boolean exists(String id) {
        T t;
        try {
            t = genericResourceService.get(getResourceTypeName(), id);
        } catch (ResourceNotFoundException e) {
            t = null;
        }
        return id != null && t != null;
    }


}
