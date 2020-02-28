package eu.einfracentral.registry.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Measurement;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MeasurementService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
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
    private final MeasurementService<Measurement, Authentication> measurementService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    PendingServiceController(PendingResourceService<InfraService> pendingServiceManager,
                             MeasurementService<Measurement, Authentication> measurementService,
                             InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
        this.measurementService = measurementService;
        this.infraServiceService = infraServiceService;
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<InfraService> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service = pendingServiceManager.get(id);
        pendingServiceManager.delete(service);
        logger.info("User '{}' deleted PendingService '{}' with id: '{}'", auth.getName(), service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "/service/id", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getService(@PathVariable String id) {
        return new ResponseEntity<>(pendingServiceManager.get(id).getService(), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/byProvider/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<InfraService>> getProviderInfraServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("providers", id);
        return new ResponseEntity<>(pendingServiceManager.getAll(ff, null), HttpStatus.OK);
    }

    @PostMapping(path = "/service", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
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

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySavePending(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = new InfraService();
        infraService.setService(service);
        try {
            infraService = pendingServiceManager.get(service.getId());
            infraService = pendingServiceManager.update(infraService, auth);
        } catch (ResourceException e) {
            logger.debug("Pending Service with id '{}' does not exist. Creating it...", service.getId());
            infraService = pendingServiceManager.add(infraService, auth);
        }
        return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/service", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySaveService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToPending(service.getId(), auth);
        InfraService infraService = pendingServiceManager.get(service.getId());
        infraService.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(infraService, auth).getService(), HttpStatus.OK);
    }


    @PutMapping(path = "/transform/active", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #json)")
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

        // validate the Service and update afterwards
        infraServiceService.validate(infraService);
        update(infraService, auth);

        // transform to active
        infraService = pendingServiceManager.transformToActive(infraService.getId(), auth);

        this.measurementService.updateAll(service.getId(), infraService.getId(), measurements, auth);

        return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
    }

    @GetMapping(path = "/rich/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<RichService> getPendingRich(@PathVariable("id") String id, Authentication auth) {
        return new ResponseEntity<>((RichService) pendingServiceManager.getPendingRich(id, auth), HttpStatus.OK);
    }
}
