package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.service.ContactInformationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping({"contactInformation"})
@Tag(name = "contact information")
public class ContactInformationController {

    private final ContactInformationService contactInformationService;

    @Autowired
    public ContactInformationController(ContactInformationService contactInformationService) {
        this.contactInformationService = contactInformationService;
    }

    @GetMapping(path = "getMy", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<String> getMy(@Parameter(hidden = true) Authentication authentication) {
        return contactInformationService.getMy(authentication);
    }

//    @PutMapping(path = "updateContactInfoTransfer", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void updateContactInfoTransfer(@RequestParam boolean acceptedTransfer, @Parameter(hidden = true) Authentication authentication) {
        contactInformationService.updateContactInfoTransfer(acceptedTransfer, authentication);
    }
}
