package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 25/07/17.
 */
public class GenericRestController<T> {

    final protected ResourceCRUDService<T> service;

    GenericRestController(ResourceCRUDService service) {
        this.service = service;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> get(@PathVariable("id") String id) {
        String id_decoded = id; //new String(Base64.getDecoder().decode(id));
        T resource = service.get(id_decoded);
        if (resource == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(resource, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> addJson(@RequestBody T resource) {
        service.add(resource, ParserService.ParserServiceTypes.JSON);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> addXml(@RequestBody T resource) {
        service.add(resource, ParserService.ParserServiceTypes.XML);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> update(@RequestBody T resource) {
        service.update(resource, ParserService.ParserServiceTypes.JSON);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> delete(@RequestBody T resource) {
        service.delete(resource);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(path = "all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Browsing> getAll(@RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = new FacetFilter();
        filter.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        filter.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        filter.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
//        Map<String,Object> sort = new HashMap<>();
//        Map<String,Object> order = new HashMap<>();
//        String orderDirection = allRequestParams.get("order") != null ? (String)allRequestParams.remove("order") : "asc";
//        String orderField = allRequestParams.get("orderField") != null ? (String)allRequestParams.remove("orderField") : null;
//        if (orderField != null) {
//            order.put("order",orderDirection);
//            sort.put(orderField, order);
//            filter.setOrderBy(sort);
//        }
        filter.setFilter(allRequestParams);
        return new ResponseEntity<>(service.getAll(filter), HttpStatus.OK);
    }

    @RequestMapping(path = "some/{ids}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<T>> getSome(@PathVariable String[] ids) {
        List<T> ret = service.getSome(ids);
        if (ret == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(ret, HttpStatus.OK);
        }
    }

    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field) {
        return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
    }

}
