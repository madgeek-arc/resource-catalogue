package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ResourceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.exception.ServerError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//the below line contains the only produces needed for spring to work in the entire project; all others are there until springfox fix their bugs
@RequestMapping(consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE}, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
public class ResourceController<T extends Identifiable, U extends Authentication> {
    protected final ResourceService<T, U> service;

    private static final Logger logger = LogManager.getLogger(ResourceController.class);

    ResourceController(ResourceService<T, U> service) {
        this.service = service;
    }

    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<T> get(@PathVariable("id") String id, @ApiIgnore U authentication) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<T> add(@RequestBody T t, @ApiIgnore U auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.add(t, auth), HttpStatus.CREATED);
        logger.debug("User " + auth.getName() + " created a new Resource with id " + t.getId());
        return ret;
    }

    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<T> update(@RequestBody T t, @ApiIgnore U auth) throws ResourceNotFoundException {
        ResponseEntity<T> ret = new ResponseEntity<>(service.update(t, auth), HttpStatus.OK);
        logger.debug("User " + auth.getName() + " updated Resource with id " + t.getId());
        return ret;
    }

    @RequestMapping(path = "validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<T> validate(@RequestBody T t, @ApiIgnore U auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.validate(t), HttpStatus.OK);
        logger.debug("User " + auth.getName() + " validated Resource with id " + t.getId());
        return ret;
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<T> delete(@RequestBody T t, @ApiIgnore U auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.del(t), HttpStatus.OK);
        logger.debug("User " + auth.getName() + " deleted Resource with id " + t.getId());
        return ret;
    }

    @RequestMapping(path = "all", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<T>> delAll(@ApiIgnore U auth) {
        ResponseEntity<List<T>> ret = new ResponseEntity<>(service.delAll(), HttpStatus.OK);
        logger.debug("User " + auth.getName() + " deleted a list of resources " + ret);
        return ret;
    }

    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<T>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore U auth) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(allRequestParams);
        return new ResponseEntity<>(service.getAll(ff, null), HttpStatus.OK);
    }

    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<T>> getSome(@PathVariable String[] ids, @ApiIgnore U auth) {
        return new ResponseEntity<>(service.getSome(ids), HttpStatus.OK);
    }

    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field, @ApiIgnore U auth) {
        return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
    }

    @ExceptionHandler(ResourceException.class)
    @ResponseBody
    public ResponseEntity<ServerError> handleResourceException(HttpServletRequest req, ResourceException e) {
        return new ResponseEntity<>(new ServerError(req.getRequestURL().toString(), e), e.getStatus());
    }

    @RequestMapping(path = {"versions/{id}", "versions/{id}/{version}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<T>> versions(@PathVariable String id, @PathVariable Optional<String> version, @ApiIgnore U auth) {
        return new ResponseEntity<>(service.versions(id, version.orElse(null)), HttpStatus.OK);
    }
}
