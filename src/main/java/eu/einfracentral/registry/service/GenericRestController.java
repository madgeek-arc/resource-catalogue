package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pgl on 25/07/17.
 */
public class GenericRestController<T> {

    final protected ResourceCRUDService<T> service;

    GenericRestController(ResourceCRUDService service) {
        this.service = service;
    }

    @RequestMapping(value = "{id}/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> getComponent(@PathVariable("id") String id) {
        String id_decoded = id; //new String(Base64.getDecoder().decode(id));
        T component = service.get(id_decoded);
        if (component == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(component, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> addComponentJson(@RequestBody T component) {
        service.add(component);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> addComponentXml(@RequestBody T component) {
        service.add(component);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<T> updateComponent(@RequestBody T component) {
        service.update(component);
        return new ResponseEntity<>(component, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> deleteComponent(@RequestBody T component) {
        service.delete(component);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(path = "all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Browsing> getAllComponents(@RequestParam Map<String,Object> allRequestParams) {
        FacetFilter filter = new FacetFilter();
        filter.setKeyword(allRequestParams.get("query") != null ? (String)allRequestParams.remove("query") : "");
        filter.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String)allRequestParams.remove("from")) : 0);
        filter.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String)allRequestParams.remove("quantity")) : 10);
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

}
