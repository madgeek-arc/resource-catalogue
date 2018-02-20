package eu.einfracentral.registry.service;

import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ServerError;
import eu.openminted.registry.core.service.ParserService;
import io.swagger.annotations.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by pgl on 25/07/17.
 */
public class ResourceController<T> {
    protected final ResourceService<T> service;

    ResourceController(ResourceService service) {
        this.service = service;
    }

    @ApiOperation(value = "Returns the resource assigned the given id.")
    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> get(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Adds the given resource.")
    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> addJSON(@RequestBody T t, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.add(t, ParserService.ParserServiceTypes.JSON), HttpStatus.OK);
    }

    @ApiOperation(value = "Adds the given resource.")
    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<T> addXML(@RequestBody T t, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.add(t, ParserService.ParserServiceTypes.XML), HttpStatus.OK);
    }

    @ApiOperation(value = "Updates the resource assigned the given id with the given resource, keeping a history of revisions.")
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> updateJSON(@RequestBody T t, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.update(t, ParserService.ParserServiceTypes.JSON), HttpStatus.OK);
    }

    @ApiOperation(value = "Updates the resource assigned the given id with the given resource, keeping a history of revisions.")
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<T> updateXML(@RequestBody T t, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.update(t, ParserService.ParserServiceTypes.XML), HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes the resource assigned the given id.")
    @RequestMapping(method = RequestMethod.DELETE, consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE}, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ApiIgnore
    public ResponseEntity<T> delete(@RequestBody T t, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.del(t), HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes all resources.")
    @RequestMapping(path = "all", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiIgnore
    public ResponseEntity<Browsing> delAll(@ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.delAll(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns all resources, satisfying the given parametres.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of resources to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Browsing> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        FacetFilter filter = new FacetFilter();
        filter.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        filter.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        filter.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        filter.setFilter(allRequestParams);
        return new ResponseEntity<>(service.getAll(filter), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of resources. If a resource isn't found, null  is returned in its stead")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of resource ids", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "some/{ids}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<T>> getSome(@PathVariable String[] ids, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.getSome(ids), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns all resources, grouped by the given field.")
    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
    }

    @ExceptionHandler(ResourceException.class)
    @ResponseBody
    public ResponseEntity<ServerError> handleRESTException(HttpServletRequest req, ResourceException e) {
        return new ResponseEntity<>(new ServerError(req.getRequestURL().toString(), e), e.getStatus());
    }
}
