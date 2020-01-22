package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.PendingResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("pendingProvider")
public class PendingProviderController extends ResourceController<ProviderBundle, Authentication> {

    private final PendingResourceService<ProviderBundle> pendingProviderService;

    @Autowired
    PendingProviderController(PendingResourceService<ProviderBundle> pendingProviderService) {
        super(pendingProviderService);
        this.pendingProviderService = pendingProviderService;
    }

    @PostMapping("/transformToPending")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformProviderToPending(@RequestParam String providerId) {
        pendingProviderService.transformToPending(providerId);
    }

    @PostMapping("/transformToActive")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformProviderToActive(@RequestParam String providerId) {
        pendingProviderService.transformToActive(providerId);
    }

    @GetMapping(path = "/getId", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> getIdFromOriginalId(@RequestParam("originalId") String originalId) {
        return new ResponseEntity<>(pendingProviderService.getId(originalId), HttpStatus.OK);
    }

    @GetMapping(path = "/getIdMappings", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Map<String, String>> getIdFromOriginalId() {
        return new ResponseEntity<>(pendingProviderService.getIdOriginalIdMap(), HttpStatus.OK);
    }
}
