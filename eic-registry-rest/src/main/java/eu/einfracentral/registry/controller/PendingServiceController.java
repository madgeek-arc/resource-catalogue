package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ResourceBundleService;
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
public class PendingServiceController extends ResourceController<ServiceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingServiceController.class);
    private final PendingResourceService<ServiceBundle> pendingServiceManager;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final IdCreator idCreator;

    @Autowired
    PendingServiceController(PendingResourceService<ServiceBundle> pendingServiceManager,
                             ResourceBundleService<ServiceBundle> resourceBundleService,
                             IdCreator idCreator) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
        this.resourceBundleService = resourceBundleService;
        this.idCreator = idCreator;
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service = pendingServiceManager.get(id);
        pendingServiceManager.delete(service);
        logger.info("User '{}' deleted Pending Resource '{}' with id: '{}'", auth.getName(), service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "/resource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> getService(@PathVariable String id) {
        return new ResponseEntity<>(pendingServiceManager.get(id).getService(), HttpStatus.OK);
    }

    @GetMapping(path = "/rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RichResource> getPendingRich(@PathVariable("id") String id, Authentication auth) {
        return new ResponseEntity<>((RichResource) pendingServiceManager.getPendingRich(id, auth), HttpStatus.OK);
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
    public ResponseEntity<Paging<ServiceBundle>> getProviderPendingServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        return new ResponseEntity<>(pendingServiceManager.getAll(ff, null), HttpStatus.OK);
    }

    @PostMapping(path = "/addResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = new ServiceBundle(service);
        return new ResponseEntity<>(pendingServiceManager.add(serviceBundle, auth).getService(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/updateResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> updateService(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = pendingServiceManager.get(service.getId());
        serviceBundle.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(serviceBundle, auth).getService(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId)")
    public void transformServiceToPending(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToPending(serviceId, auth);
    }

    @PostMapping("/transform/resource")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId)")
    public void transformServiceToInfra(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToActive(serviceId, auth);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySavePending(@RequestBody Service service, @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = new ServiceBundle();
        ServiceBundle toCreateId = new ServiceBundle();
        toCreateId.setService(service);
        service.setId(idCreator.createResourceId(toCreateId));
        try {
            serviceBundle = pendingServiceManager.get(service.getId());
            serviceBundle.setService(service);
            serviceBundle = pendingServiceManager.update(serviceBundle, auth);
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.debug("Pending Resource with id '{}' does not exist. Creating it...", service.getId());
            serviceBundle.setService(service);
            serviceBundle = pendingServiceManager.add(serviceBundle, auth);
        }
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySaveService(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        pendingServiceManager.transformToPending(service.getId(), auth);
        ServiceBundle serviceBundle = pendingServiceManager.get(service.getId());
        serviceBundle.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(serviceBundle, auth).getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/transform/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> pendingToInfra(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        if (service == null) {
            throw new ServiceException("Cannot add a null Resource");
        }
        ServiceBundle serviceBundle = null;

        try { // check if service already exists
            ServiceBundle toCreateId = new ServiceBundle();
            toCreateId.setService(service);
            service.setId(idCreator.createResourceId(toCreateId));
            serviceBundle = this.pendingServiceManager.get(service.getId());
        } catch (ResourceException | eu.einfracentral.exception.ResourceNotFoundException e) {
            // continue with the creation of the service
        }

        if (serviceBundle == null) { // if existing Pending Service is null, create a new Active Service
            serviceBundle = resourceBundleService.addResource(new ServiceBundle(service), auth);
            logger.info("User '{}' added Resource:\n{}", auth.getName(), serviceBundle);
        } else { // else update Pending Service and transform it to Active Service
            if (serviceBundle.getService().getVersion().equals("")){
                serviceBundle.getService().setVersion(null);
            }
            serviceBundle.setService(service); // important to keep other fields of ServiceBundle
            serviceBundle = pendingServiceManager.update(serviceBundle, auth);
            logger.info("User '{}' updated Pending Resource:\n{}", auth.getName(), serviceBundle);

            // transform to active
            serviceBundle = pendingServiceManager.transformToActive(serviceBundle.getId(), auth);
        }

        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }
}
