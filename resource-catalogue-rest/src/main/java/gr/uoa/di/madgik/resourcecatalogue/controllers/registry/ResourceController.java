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

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
public abstract class ResourceController<T extends Identifiable> {
    protected final ResourceService<T> service;

    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    protected ResourceController(ResourceService<T> service) {
        this.service = service;
    }

    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> get(@PathVariable("id") String id, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> add(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.add(t, auth), HttpStatus.CREATED);
        logger.debug("Created a new {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> update(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.update(t, auth), HttpStatus.OK);
        logger.debug("Updated {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> validate(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.validate(t), HttpStatus.OK);
        logger.debug("Validated {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> delete(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        service.delete(t);
        logger.debug("Deleted {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        return new ResponseEntity<>(t, HttpStatus.OK);
    }

    @DeleteMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<T>> delAll(@Parameter(hidden = true) Authentication auth) {
        ResponseEntity<List<T>> ret = new ResponseEntity<>(service.delAll(), HttpStatus.OK);
        logger.debug("Deleted a list of resources {}", ret);
        return ret;
    }

    // Filter a list of Resources based on a set of filters.
    @BrowseParameters
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<T>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        return new ResponseEntity<>(service.getAll(ff, null), HttpStatus.OK);
    }

    @GetMapping(path = "ids/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<T>> getSome(@PathVariable String[] ids, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.getSome(ids), HttpStatus.OK);
    }

    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
    }

//    @ExceptionHandler(ResourceException.class)
//    @ResponseBody
//    public ResponseEntity<ServerError> handleResourceException(HttpServletRequest req, ResourceException e) {
//        return new ResponseEntity<>(new ServerError(req.getRequestURL().toString(), e), e.getStatus());
//    }
}
