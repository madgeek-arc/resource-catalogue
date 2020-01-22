package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.PendingResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
@RequestMapping("pendingService")
public class PendingServiceController extends ResourceController<InfraService, Authentication> {

    private final PendingResourceService<InfraService> pendingServiceManager;

    @Autowired
    PendingServiceController(PendingResourceService<InfraService> pendingServiceManager) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
    }

    @PostMapping(path = "/addService", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = new InfraService(service);
        return new ResponseEntity<>(pendingServiceManager.add(infraService, auth).getService(), HttpStatus.CREATED);
    }

    @PostMapping("/transformToPending")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToPending(@RequestParam String serviceId) {
        pendingServiceManager.transformToPending(serviceId);
    }

    @PostMapping("/transformToInfra")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToInfra(@RequestParam String serviceId) {
        pendingServiceManager.transformToActive(serviceId);
    }

    @GetMapping(path = "/rich/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<RichService> getPendingRich(@PathVariable("id") String id, Authentication auth) {
        return new ResponseEntity<>((RichService) pendingServiceManager.getPendingRich(id, auth), HttpStatus.OK);
    }

    @GetMapping(path = "/getId", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> getIdFromOriginalId(@RequestParam("originalId") String originalId) {
        return new ResponseEntity<>(pendingServiceManager.getId(originalId), HttpStatus.OK);
    }

    @GetMapping(path = "/getIdMappings", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Map<String, String>> getIdFromOriginalId() {
        return new ResponseEntity<>(pendingServiceManager.getIdOriginalIdMap(), HttpStatus.OK);
    }
}
