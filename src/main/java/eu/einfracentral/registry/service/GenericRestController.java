package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 25/07/17.
 */
public class GenericRestController<T> {

    final protected ResourceCRUDService<T> service;

    GenericRestController(ResourceCRUDService service) {
        this.service = service;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public ResponseEntity<T> getComponent(@PathVariable("id") String id) {
        String id_decoded = id; //new String(Base64.getDecoder().decode(id));
        T component = service.get(id_decoded);
        if (component == null)
            throw new ResourceNotFoundException();
        else
            return new ResponseEntity<>(component, HttpStatus.OK);

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

    @RequestMapping(method = RequestMethod.PUT, headers = "Accept=application/json; charset=utf-8")
    public ResponseEntity<T> updateComponent(@RequestBody T component) {
        service.update(component);
        return new ResponseEntity<>(component, HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.DELETE, headers = "Accept=application/json; charset=utf-8")
    public ResponseEntity<String> deleteComponent(@RequestBody T component) {
        service.delete(component);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @RequestMapping(path = "all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Browsing> getAllComponents(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "from", required = false, defaultValue = "0") int from,
            @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity
    ) {
        FacetFilter filter = new FacetFilter();
        filter.setFrom(from);
        filter.setQuantity(quantity);
        filter.setKeyword(keyword);
        return new ResponseEntity<>(service.getAll(filter), HttpStatus.OK);
    }

}
