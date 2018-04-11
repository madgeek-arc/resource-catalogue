package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.ProviderService;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by pgl on 26/07/17.
 */
@RestController
@RequestMapping("provider")
public class ProviderController extends ResourceController<Provider> {
    @Autowired
    ProviderController(ProviderService service) {
        super(service);
    }

    @ApiOperation(value = "Returns the provider visits")
    @RequestMapping(value = "visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(((ProviderService) service).visits(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider favourites")
    @RequestMapping(value = "favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> favourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(((ProviderService) service).favourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider ratings")
    @RequestMapping(value = "ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> ratings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(((ProviderService) service).ratings(id), HttpStatus.OK);
    }
}
