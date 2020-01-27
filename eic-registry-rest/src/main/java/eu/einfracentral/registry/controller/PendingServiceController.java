package eu.einfracentral.registry.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Measurement;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.MeasurementService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("pendingService")
public class PendingServiceController extends ResourceController<InfraService, Authentication> {

    private static Logger logger = LogManager.getLogger(PendingServiceController.class);
    private final PendingResourceService<InfraService> pendingServiceManager;
    private MeasurementService<Measurement, Authentication> measurementService;

    @Autowired
    PendingServiceController(PendingResourceService<InfraService> pendingServiceManager, MeasurementService<Measurement, Authentication> measurementService) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
        this.measurementService = measurementService;
    }

    @GetMapping(path = "/service/id", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Service> getService(@PathVariable String id) {
        return new ResponseEntity<>(pendingServiceManager.get(id).getService(), HttpStatus.OK);
    }

    @GetMapping(path = "/byProvider/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Service>> getPendingServices(@PathVariable String id) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", id);
        ff.setQuantity(10000);
        return new ResponseEntity<>(pendingServiceManager.getAll(ff, null)
                .getResults()
                .stream()
                .map(InfraService::getService)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @PostMapping(path = "/service", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = new InfraService(service);
        return new ResponseEntity<>(pendingServiceManager.add(infraService, auth).getService(), HttpStatus.CREATED);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToPending(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToPending(serviceId, auth);
    }

    @PostMapping("/transform/active")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToInfra(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToActive(serviceId, auth);
    }

    @PutMapping(path = "/transform/active", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("isAuthenticated()") // FIXME: authorize only admins and service providers
    public ResponseEntity<Service> pendingToInfra(@RequestBody Map<String, JsonNode> json, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Service service = null;
        List<Measurement> measurements = new ArrayList<>();
        try {
            service = mapper.readValue(json.get("service").toString(), Service.class);
            measurements = Arrays.stream(mapper.readValue(json.get("measurements").toString(), Measurement[].class)).collect(Collectors.toList());

        } catch (JsonParseException e) {
            logger.error("JsonParseException", e);
        } catch (JsonMappingException e) {
            logger.error("JsonMappingException", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        }

        if (service == null) {
            throw new ServiceException("Cannot add a null service");
        }
        InfraService infraService = pendingServiceManager.get(service.getId());
        infraService.setService(service);

        // updated InfraService and transforms to active ( may change InfraService id )
        infraService = pendingServiceManager.transformToActive(infraService, auth);

        this.measurementService.updateAll(service.getId(), infraService.getId(), measurements, auth);

        return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
    }

    @GetMapping(path = "/rich/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<RichService> getPendingRich(@PathVariable("id") String id, Authentication auth) {
        return new ResponseEntity<>((RichService) pendingServiceManager.getPendingRich(id, auth), HttpStatus.OK);
    }
}
