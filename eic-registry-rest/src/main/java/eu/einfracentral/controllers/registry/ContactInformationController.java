package eu.einfracentral.controllers.registry;

import eu.einfracentral.registry.service.ContactInformationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequestMapping("contactInformation")
@Api(value = "Contact Information Transfer")
public class ContactInformationController {

    private final ContactInformationService contactInformationService;

    @Autowired
    public ContactInformationController(ContactInformationService contactInformationService) {
        this.contactInformationService = contactInformationService;
    }

    @ApiOperation(value = "Get a list of Catalogues and Providers in which the User is Admin")
    @GetMapping(path = "getMy", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<String> getMy(@ApiIgnore Authentication authentication) {
        return contactInformationService.getMy(authentication);
    }

    @ApiOperation(value = "Update the list of ContactInfoTransfer")
    @PutMapping(path = "updateContactInfoTransfer", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void updateContactInfoTransfer(@RequestParam boolean acceptedTransfer, @ApiIgnore Authentication authentication) {
        contactInformationService.updateContactInfoTransfer(acceptedTransfer, authentication);
    }
}
