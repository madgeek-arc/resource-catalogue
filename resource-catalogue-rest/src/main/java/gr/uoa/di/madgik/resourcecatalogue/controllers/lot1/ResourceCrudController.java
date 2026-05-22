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

package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.controllers.AbstractGenericController;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

@RequestMapping
public abstract class ResourceCrudController<T> {

    /** The service used to perform all read and write operations on the registry. */
    protected final GenericResourceService service;

    /**
     * Constructs the controller with the required service dependency.
     *
     * @param genericResourceService the registry service; must not be {@code null}
     */
    protected ResourceCrudController(GenericResourceService genericResourceService) {
        this.service = genericResourceService;
    }

    /**
     * Returns the registry resource-type name that this controller manages.
     *
     * <p>The value must match the {@code name} attribute of a registered {@code ResourceType}.
     * It is used as the implicit {@code resourceType} argument for every service call.
     *
     * @return the resource-type name, never {@code null} or empty
     */
    protected abstract String getResourceTypeName();

    private String extractPid(String id, HttpServletRequest request) {
        String restOfThePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, restOfThePath);

        id = id + (arguments.isEmpty() ? "" : "/" + arguments);
        return id;
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> get(@PathVariable(value = "id") String id,
                                 @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>((T) service.get(getResourceTypeName(), id), HttpStatus.OK);
    }

    /**
     * <p>Hack to get the pid value containing the slash ('/') character in the url.</p>
     * <p>It uses the {@link HttpServletRequest} to retrieve the whole pid from the url.</p>
     * <p>The method is annotated as {@link Hidden} to avoid issues with swagger. Swagger picks up the trailing
     * wildcard characters ('*') as part of the path, so the swagger API calls always contain the '/**' suffix.
     * </p>
     *
     * @param id             the prefix of the pid
     * @param authentication the user authentication
     * @param request        the HttpServletRequest
     * @return the resource identified by the provided pid
     */
    @Hidden
    @GetMapping(path = "{id}/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> getByPid(@PathVariable("id") @Parameter(allowReserved = true) String id,
                                      @Parameter(hidden = true) Authentication authentication,
                                      HttpServletRequest request) {
        return new ResponseEntity<>((T) service.get(getResourceTypeName(), extractPid(id, request)), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> add(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.add(getResourceTypeName(), t), HttpStatus.CREATED);
    }

    @PostMapping(path = "/bulk")
    public void addBulk(@RequestBody List<T> bundles, @Parameter(hidden = true) Authentication auth) {
        for (T resource : bundles) {
            this.add(resource, auth);
        }
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> update(@RequestBody T t,
                                    @Parameter(hidden = true) Authentication auth) {
        if (!service.exists(getResourceTypeName(), t))
            throw new ResourceNotFoundException();
        return new ResponseEntity<>(service.update(getResourceTypeName(), t), HttpStatus.OK);
    }

    @PostMapping(path = "validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> validate(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        service.validate(getResourceTypeName(), t); // TODO : Configure resource validator
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> delete(@PathVariable String id, @Parameter(hidden = true) Authentication auth) {
        service.delete(getResourceTypeName(), id);
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    @Hidden
    @DeleteMapping(path = "{id}/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> deleteByPid(@PathVariable("id") @Parameter(allowReserved = true) String id,
                                         @Parameter(hidden = true) Authentication authentication,
                                         HttpServletRequest request) {
        service.delete(getResourceTypeName(), extractPid(id, request));
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    // Filter a list of Resources based on a set of filters.
    @BrowseParameters
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<T>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType(getResourceTypeName());
        return new ResponseEntity<>(service.getResults(ff), HttpStatus.OK);
    }
}
