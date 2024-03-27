package eu.einfracentral.controllers.registry;

import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
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
@RequestMapping({"serviceBundle"})
@Api(value = "Get Information about a Service Bundle")
public class ServiceBundleController {

    private static final Logger logger = LogManager.getLogger(ServiceBundleController.class.getName());
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final GenericResourceService genericResourceService;

    @Autowired
    ServiceBundleController(ServiceBundleService<ServiceBundle> serviceBundleService,
                            GenericResourceService genericResourceService) {
        this.serviceBundleService = serviceBundleService;
        this.genericResourceService = genericResourceService;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id,
                                                @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ServiceBundle service;
        service = serviceBundleService.get(id, catalogueId);
        if (service == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        serviceBundleService.delete(service);
        logger.info("User '{}' deleted ServiceBundle '{}' with id: '{}' of the Catalogue: '{}'", authentication, service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    @DeleteMapping(path = "delete/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> deleteAll(@ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ServiceBundle> services = serviceBundleService.getAll(ff, null).getResults();
        for (ServiceBundle service : services) {
            logger.info("Deleting service with name: {}", service.getService().getName());
            serviceBundleService.delete(service);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> get(@PathVariable("id") String id,
                                 @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                 @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(serviceBundleService.get(id, catalogueId), HttpStatus.OK);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> add(@RequestBody ServiceBundle service, Authentication authentication) {
        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(serviceBundleService.add(service, authentication), HttpStatus.OK);
        logger.info("User '{}' added ServiceBundle '{}' with id: {} and version: {}", authentication, service.getService().getName(), service.getService().getId(), service.getService().getVersion());
        logger.info(" Service Organisation: {}", service.getService().getResourceOrganisation());
        return ret;
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> update(@RequestBody ServiceBundle service, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(serviceBundleService.update(service, authentication), HttpStatus.OK);
        logger.info("User '{}' updated ServiceBundle '{}' with id: {}", authentication, service.getService().getName(), service.getService().getId());
        return ret;
    }

    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody ServiceBundle service, @ApiIgnore Authentication auth) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(serviceBundleService.validate(service));
        logger.info("Validating ServiceBundle: {}", service.getService().getName());
        return ret;
    }

    @Browse
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<?>> getAll(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                            @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                            @ApiIgnore Authentication authentication) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Map<String, List<ServiceBundle>>> getBy(@PathVariable String field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        return ResponseEntity.ok(serviceBundleService.getBy(field, auth));
    }

    @PatchMapping(path = "publish/{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> setActive(@PathVariable String id, @PathVariable String version,
                                                   @RequestParam boolean active,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service = serviceBundleService.get(id, version);
        service.setActive(active);
        Metadata metadata = service.getMetadata();
        metadata.setModifiedBy("system");
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        service.setMetadata(metadata);
        if (active) {
            logger.info("User '{}' set ServiceBundle '{}' with id: {} as active", auth.getName(), service.getService().getName(), service.getService().getId());
        } else {
            logger.info("User '{}' set ServiceBundle '{}' with id: {} as inactive", auth.getName(), service.getService().getName(), service.getService().getId());
        }
        return ResponseEntity.ok(serviceBundleService.update(service, auth));
    }

}
