package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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

    @GetMapping(path = "/provider/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(pendingProviderService.get(id).getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "/id", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> getIdFromOriginalId(@RequestParam("originalId") String originalId) {
        return new ResponseEntity<>(pendingProviderService.getId(originalId), HttpStatus.OK);
    }

    @GetMapping(path = "/id/mappings", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Map<String, String>> getIdFromOriginalId() {
        return new ResponseEntity<>(pendingProviderService.getIdOriginalIdMap(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformProviderToPending(@RequestParam String providerId, @ApiIgnore Authentication auth) {
        pendingProviderService.transformToPending(providerId, auth);
    }

    @PostMapping("/transform/active")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformProviderToActive(@RequestParam String providerId, @ApiIgnore Authentication auth) {
        pendingProviderService.transformToActive(providerId, auth);
    }

    @PutMapping(path = "/transform/active", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Provider> updateAndPublish(@RequestBody Provider provider, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = pendingProviderService.get(provider.getId());
        providerBundle.setProvider(provider);

        providerBundle.setProvider(provider);
        update(providerBundle, auth);

        // updates provider and transforms to active ( may change provider id and all of its services ids )
        providerBundle = pendingProviderService.transformToActive(providerBundle.getId(), auth);

//        // updates provider and transforms to active ( may change provider id and all of its services ids )
//        providerBundle = pendingProviderService.transformToActive(providerBundle, auth);
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }
}
