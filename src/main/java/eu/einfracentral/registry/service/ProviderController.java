package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by pgl on 26/07/17.
 */
@RestController
@RequestMapping("provider")
public class ProviderController extends GenericRestController<Provider> {

    @Autowired
    ProviderController(ProviderService service) {
        super(service);
    }

    @RequestMapping(path = "hard", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String[]> getHard() {
        return new ResponseEntity<>(new String[]{"EGI Foundation", "EUDAT CDI consortium", "GÉANT", "OpenAIRE consortium", "PRACE"}, HttpStatus.OK);
    }

}