package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import io.swagger.annotations.Api;
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

@RestController
@RequestMapping({"pendingResource", "pendingService"})
@Api(description = "Operations for Pending Resources/Services", tags = {"pending-resource-controller"})
public class PendingServiceController extends ResourceController<InfraService, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingServiceController.class);
    private final PendingResourceService<InfraService> pendingServiceManager;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final IdCreator idCreator;

    @Autowired
    PendingServiceController(PendingResourceService<InfraService> pendingServiceManager,
                             InfraServiceService<InfraService, InfraService> infraServiceService,
                             IdCreator idCreator) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
        this.infraServiceService = infraServiceService;
        this.idCreator = idCreator;
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<InfraService> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service = pendingServiceManager.get(id);
        pendingServiceManager.delete(service);
        logger.info("User '{}' deleted Pending Resource '{}' with id: '{}'", auth.getName(), service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "/resource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> getService(@PathVariable String id) {
        return new ResponseEntity<>(pendingServiceManager.get(id).getService(), HttpStatus.OK);
    }

    @GetMapping(path = "/rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RichService> getPendingRich(@PathVariable("id") String id, Authentication auth) {
        return new ResponseEntity<>((RichService) pendingServiceManager.getPendingRich(id, auth), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<InfraService>> getProviderPendingServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        return new ResponseEntity<>(pendingServiceManager.getAll(ff, null), HttpStatus.OK);
    }

    @PostMapping(path = "/addResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = new InfraService(service);
        return new ResponseEntity<>(pendingServiceManager.add(infraService, auth).getService(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/updateResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> updateService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = pendingServiceManager.get(service.getId());
        infraService.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(infraService, auth).getService(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #serviceId)")
    public void transformServiceToPending(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToPending(serviceId, auth);
    }

    @PostMapping("/transform/resource")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #serviceId)")
    public void transformServiceToInfra(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToActive(serviceId, auth);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySavePending(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService infraService = new InfraService();
        try {
            infraService = pendingServiceManager.get(service.getId());
            infraService.setService(service);
            infraService = pendingServiceManager.update(infraService, auth);
        } catch (ResourceException e) {
            logger.debug("Pending Resource with id '{}' does not exist. Creating it...", service.getId());
            infraService.setService(service);
            infraService = pendingServiceManager.add(infraService, auth);
        }
        return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySaveService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToPending(service.getId(), auth);
        InfraService infraService = pendingServiceManager.get(service.getId());
        infraService.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(infraService, auth).getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/transform/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #service)")
    public ResponseEntity<Service> pendingToInfra(@RequestBody Service service, @ApiIgnore Authentication auth) {
        if (service == null) {
            throw new ServiceException("Cannot add a null Resource");
        }
        InfraService infraService = null;

        try { // check if service already exists
            if (service.getId() == null || "".equals(service.getId())) { // if service id is not given, create it
                service.setId(idCreator.createServiceId(service));
            }
            infraService = this.pendingServiceManager.get(service.getId());
        } catch (ResourceException | eu.einfracentral.exception.ResourceNotFoundException e) {
            // continue with the creation of the service
        }

        if (infraService == null) { // if existing Pending Service is null, create a new Active Service
            infraService = infraServiceService.addService(new InfraService(service), auth);
            logger.info("User '{}' added Resource:\n{}", auth.getName(), infraService);
        } else { // else update Pending Service and transform it to Active Service
            infraService.setService(service); // important to keep other fields of InfraService
            infraService = pendingServiceManager.update(infraService, auth);
            logger.info("User '{}' updated Pending Resource:\n{}", auth.getName(), infraService);

            // transform to active
            infraService = pendingServiceManager.transformToActive(infraService.getId(), auth);
        }

        return new ResponseEntity<>(infraService.getService(), HttpStatus.OK);
    }
}
