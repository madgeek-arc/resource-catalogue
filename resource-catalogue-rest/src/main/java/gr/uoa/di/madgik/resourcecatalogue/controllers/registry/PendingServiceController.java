package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;


@Profile("beyond")
@RestController
@RequestMapping({"pendingService"})
@Tag(name = "pending service")
public class PendingServiceController extends ResourceController<ServiceBundle> {

    private static final Logger logger = LogManager.getLogger(PendingServiceController.class);
    private final PendingResourceService<ServiceBundle> pendingServiceManager;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final ProviderService<ProviderBundle> providerService;
    private GenericResourceService genericResourceService;
    private final IdCreator idCreator;

    @Value("${catalogue.id}")
    private String catalogueId;

    PendingServiceController(PendingResourceService<ServiceBundle> pendingServiceManager,
                             ServiceBundleService<ServiceBundle> serviceBundleService,
                             ProviderService<ProviderBundle> providerService,
                             GenericResourceService genericResourceService,
                             IdCreator idCreator) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
        this.serviceBundleService = serviceBundleService;
        this.providerService = providerService;
        this.genericResourceService = genericResourceService;
        this.idCreator = idCreator;
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service = pendingServiceManager.get(id);
        pendingServiceManager.delete(service);
        logger.info("User '{}' deleted Pending Resource '{}' with id: '{}'", User.of(auth).getEmail(), service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getService(@PathVariable String id) {
        return new ResponseEntity<>(pendingServiceManager.get(id).getService(), HttpStatus.OK);
    }

    @Browse
    @GetMapping(path = "/byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<ServiceBundle>> getProviderPendingServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                            @PathVariable String id,
                                                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("pending_service");
        ff.addFilter("published", false);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PostMapping(path = "/addResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Service> addService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = new ServiceBundle(service);
        return new ResponseEntity<>(pendingServiceManager.add(serviceBundle, auth).getService(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/updateResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> updateService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = pendingServiceManager.get(service.getId());
        serviceBundle.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(serviceBundle, auth).getService(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId)")
    public void transformServiceToPending(@RequestParam String serviceId, @Parameter(hidden = true) Authentication auth) {
        pendingServiceManager.transformToPending(serviceId, auth);
    }

    @PostMapping("/transform/resource")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId)")
    public void transformServiceToInfra(@RequestParam String serviceId, @Parameter(hidden = true) Authentication auth) {
        pendingServiceManager.transformToActive(serviceId, auth);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PostAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, returnObject.body)")
    public ResponseEntity<Service> temporarySavePending(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = new ServiceBundle();
        ServiceBundle toCreateId = new ServiceBundle();
        toCreateId.setService(service);
        service.setId(idCreator.generate("ser"));
        try {
            serviceBundle = pendingServiceManager.get(service.getId());
            serviceBundle.setService(service);
            serviceBundle = pendingServiceManager.update(serviceBundle, auth);
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.debug("Pending Resource with id '{}' does not exist. Creating it...", service.getId());
            serviceBundle.setService(service);
            serviceBundle = pendingServiceManager.add(serviceBundle, auth);
        }
        logger.info("User '{}' saved a Draft Service with id '{}'", User.of(auth).getEmail(), service.getId());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> temporarySaveService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        pendingServiceManager.transformToPending(service.getId(), auth);
        ServiceBundle serviceBundle = pendingServiceManager.get(service.getId());
        serviceBundle.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(serviceBundle, auth).getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/transform/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #service)")
    public ResponseEntity<Service> pendingToInfra(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        if (service == null) {
            throw new ServiceException("Cannot add a null Resource");
        }
        ServiceBundle serviceBundle = null;

        try { // check if service already exists
            serviceBundle = this.pendingServiceManager.get(service.getId());
        } catch (ResourceException e) {
            // continue with the creation of the service
        }

        // check Provider's template status -> block transform if it's on 'pending' state
        String resourceOrganisation = service.getResourceOrganisation();
        ProviderBundle providerBundle = providerService.get(resourceOrganisation);
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ValidationException(String.format("There is already a Resource waiting to be approved for the Provider [%s]", resourceOrganisation));
        }

        if (serviceBundle == null) { // if existing Pending Service is null, create a new Active Service
            serviceBundle = serviceBundleService.addResource(new ServiceBundle(service), auth);
            logger.info("User '{}' added Resource:\n{}", User.of(auth).getEmail(), serviceBundle);
        } else { // else update Pending Service and transform it to Active Service
            if (serviceBundle.getService().getVersion() != null && serviceBundle.getService().getVersion().equals("")) {
                serviceBundle.getService().setVersion(null);
            }
            serviceBundle.setService(service); // important to keep other fields of ServiceBundle
            serviceBundle = pendingServiceManager.update(serviceBundle, auth);
            logger.info("User '{}' updated Pending Resource:\n{}", User.of(auth).getEmail(), serviceBundle);

            // transform to active
            serviceBundle = pendingServiceManager.transformToActive(serviceBundle.getId(), auth);
        }

        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }
}
