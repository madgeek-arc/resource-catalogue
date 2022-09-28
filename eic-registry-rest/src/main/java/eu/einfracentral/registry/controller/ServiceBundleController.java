package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"infraService", "serviceBundle"})
@Api(value = "Get Information about a Service")
public class ServiceBundleController {

    private static final Logger logger = LogManager.getLogger(ServiceBundleController.class.getName());
    private final ResourceBundleService<ServiceBundle> infraService;

    @Autowired
    ServiceBundleController(ResourceBundleService<ServiceBundle> service) {
        this.infraService = service;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id,
                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ServiceBundle service;
        service = infraService.get(id, catalogueId);
        if (service == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        infraService.delete(service);
        logger.info("User '{}' deleted ServiceBundle '{}' with id: '{}' of the Catalogue: '{}'", authentication, service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    @DeleteMapping(path = "delete/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> deleteAll(@ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ServiceBundle> services = infraService.getAll(ff, null).getResults();
        for (ServiceBundle service : services) {
            logger.info("Deleting service with name: {}", service.getService().getName());
            infraService.delete(service);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> get(@PathVariable("id") String id,
                                             @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                             @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.get(id, catalogueId), HttpStatus.OK);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> add(@RequestBody ServiceBundle service, Authentication authentication) {
        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(infraService.add(service, authentication), HttpStatus.OK);
        logger.info("User '{}' added ServiceBundle '{}' with id: {} and version: {}", authentication, service.getService().getName(), service.getService().getId(), service.getService().getVersion());
        logger.info(" Service Organisation: {}", service.getService().getResourceOrganisation());
        return ret;
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> update(@RequestBody ServiceBundle service, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(infraService.update(service, authentication), HttpStatus.OK);
        logger.info("User '{}' updated ServiceBundle '{}' with id: {}", authentication, service.getService().getName(), service.getService().getId());
        return ret;
    }

    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody ServiceBundle service, @ApiIgnore Authentication auth) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(infraService.validate(service));
        logger.info("Validating ServiceBundle: {}", service.getService().getName());
        return ret;
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAll(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        return ResponseEntity.ok(infraService.getAll(ff, authentication));
    }

    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Map<String, List<ServiceBundle>>> getBy(@PathVariable String field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        return ResponseEntity.ok(infraService.getBy(field, auth));
    }

    @PatchMapping(path = "publish/{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> setActive(@PathVariable String id, @PathVariable String version,
                                                   @RequestParam boolean active,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service = infraService.get(id, version);
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
        return ResponseEntity.ok(infraService.update(service, auth));
    }

}