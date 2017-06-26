package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.domain.BaseMetadataRecord;
import eu.openminted.registry.domain.Browsing;
import eu.openminted.registry.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * Created by stefanos on 20/6/2017.
 */

public class GenericRestController<T extends BaseMetadataRecord> {

    final protected ResourceCRUDService<T> service;

    GenericRestController(ResourceCRUDService service) {
        this.service = service;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public ResponseEntity<T> getComponent(@PathVariable("id") String id) {
        String id_decoded = new String(Base64.getDecoder().decode(id));
        T component = service.get(id_decoded);
        if (component == null)
            throw new ResourceNotFoundException();
        else
            return new ResponseEntity<>(component, HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.POST, headers = "Accept=application/json; charset=utf-8")
    public ResponseEntity<String> addComponentJson(@RequestBody T component) {
        service.add(component);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.POST, headers = "Accept=application/xml; charset=utf-8")
    public ResponseEntity<String> addComponentXml(@RequestBody T component) {
        service.add(component);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.PUT, headers = "Accept=application/json; charset=utf-8")
    public ResponseEntity<String> updateComponent(@RequestBody T component) {
        service.update(component);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.DELETE, headers = "Accept=application/json; charset=utf-8")
    public ResponseEntity<String> deleteComponent(@RequestBody T component) {
        service.delete(component);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @RequestMapping(path = "all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(path = "my", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Browsing> getMyComponents(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "from", required = false, defaultValue = "0") int from,
            @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity
    ) {
        FacetFilter filter = new FacetFilter();
        filter.setFrom(from);
        filter.setQuantity(quantity);
        filter.setKeyword(keyword);
        return new ResponseEntity<>(service.getMy(filter), HttpStatus.OK);
    }
}
