package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.IdCreator;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("pendingProvider")
@Api(value = "Get information about a Pending Provider")
public class PendingProviderController extends ResourceController<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingProviderController.class);

    private final PendingResourceService<ProviderBundle> pendingProviderService;
    private final ProviderService<ProviderBundle, Authentication> providerManager;
    private final IdCreator idCreator;

    @Autowired
    PendingProviderController(PendingResourceService<ProviderBundle> pendingProviderService,
                              ProviderService<ProviderBundle, Authentication> providerManager,
                              IdCreator idCreator) {
        super(pendingProviderService);
        this.pendingProviderService = pendingProviderService;
        this.providerManager = providerManager;
        this.idCreator = idCreator;
    }

    @GetMapping(path = "/provider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(pendingProviderService.get(id).getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "/id", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getIdFromOriginalId(@RequestParam("originalId") String originalId) {
        return new ResponseEntity<>(pendingProviderService.getId(originalId), HttpStatus.OK);
    }

    @GetMapping(path = "/id/mappings", produces = {MediaType.APPLICATION_JSON_VALUE})
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

    @PutMapping(path = "/transform/active", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> updateAndPublish(@RequestBody Provider provider, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = pendingProviderService.get(provider.getId());
        providerBundle.setProvider(provider);

        // validate the Provider and update afterwards ( update may change provider id and all of its services ids )
        providerManager.validate(providerBundle);
        update(providerBundle, auth);

        // transform to active
        providerBundle = pendingProviderService.transformToActive(providerBundle.getId(), auth);

        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> temporarySavePending(@RequestBody Provider provider, @ApiIgnore Authentication auth) {
        ProviderBundle bundle = new ProviderBundle();
        if (provider.getId() == null) {
            provider.setId(idCreator.createProviderId(provider));
        }
        try {
            bundle = pendingProviderService.get(provider.getId());
            bundle.setProvider(provider);
            bundle = pendingProviderService.update(bundle, auth);
        } catch (ResourceException e) {
            logger.debug("Pending Provider with id '{}' does not exist. Creating it...", provider.getId());
            bundle.setProvider(provider);
            bundle = pendingProviderService.add(bundle, auth);
        }
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth, #provider)")
    public ResponseEntity<Provider> temporarySaveProvider(@RequestBody Provider provider, @ApiIgnore Authentication auth) {
        pendingProviderService.transformToPending(provider.getId(), auth);
        ProviderBundle bundle = pendingProviderService.get(provider.getId());
        bundle.setProvider(provider);
        return new ResponseEntity<>(pendingProviderService.update(bundle, auth).getProvider(), HttpStatus.OK);
    }

    // Get a list of Providers in which you are admin.
    @GetMapping(path = "getMyPendingProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyPendingProviders(@ApiIgnore Authentication auth) {
        return new ResponseEntity<>(pendingProviderService.getMy(auth), HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String providerId, @ApiIgnore Authentication authentication){
        return pendingProviderService.hasAdminAcceptedTerms(providerId, authentication);
    }

}
