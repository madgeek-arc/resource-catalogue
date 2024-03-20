package eu.einfracentral.controllers.registry;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.service.ContactInformationService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.annotations.ApiIgnore;

public class ContactInformationController {

    private final ContactInformationService<Bundle, Authentication> contactInformationService;

    public ContactInformationController(ContactInformationService<Bundle, Authentication> contactInformationService) {
        this.contactInformationService = contactInformationService;
    }

    @ApiOperation(value = "Given a HLE, get all Providers associated with it")
    @PutMapping(path = "updateContactInfoTransfer", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or hasRole('ROLE_PROVIDER')")
    public void updateContactInfoTransfer(@RequestParam boolean acceptedTransfer, @ApiIgnore Authentication authentication) {
        contactInformationService.updateContactInfoTransfer(acceptedTransfer, authentication);
    }
}
