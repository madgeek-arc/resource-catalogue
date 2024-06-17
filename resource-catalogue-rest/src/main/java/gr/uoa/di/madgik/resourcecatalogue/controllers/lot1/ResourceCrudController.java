package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RequestMapping
public abstract class ResourceCrudController<T extends Identifiable> {
    protected final ResourceService<T> service;

    private static final Logger logger = LogManager.getLogger(ResourceCrudController.class);

    protected ResourceCrudController(ResourceService<T> service) {
        this.service = service;
    }

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
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
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
        return new ResponseEntity<>(service.get(extractPid(id, request)), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> add(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        if (service.exists(t))
            throw new ResourceAlreadyExistsException();
        ResponseEntity<T> ret = new ResponseEntity<>(service.save(t), HttpStatus.CREATED);
        logger.debug("Created a new {} with id {}", t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> update(@RequestBody T t,
                                    @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        if (!service.exists(t))
            throw new ResourceNotFoundException();
        ResponseEntity<T> ret = new ResponseEntity<>(service.save(t), HttpStatus.OK);
        logger.debug("Updated {} with id {}", t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @PostMapping(path = "validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> validate(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        service.validate(t);
        logger.debug("Validated {} with id {}", t.getClass().getSimpleName(), t.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> delete(@PathVariable String id, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        T resource = service.get(id);
        service.delete(resource);
        logger.debug("Deleted {} with id {}", resource.getClass().getSimpleName(), resource.getId());
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    // Filter a list of Resources based on a set of filters.
    @Browse
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<T>> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        return new ResponseEntity<>(service.getAll(ff), HttpStatus.OK);
    }

    @GetMapping(path = "ids", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<T>> getSome(@RequestParam("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.getSome(ids), HttpStatus.OK);
    }

    @GetMapping(path = "by/{field}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
    }
}
