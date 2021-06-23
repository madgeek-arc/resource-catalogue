package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.dto.UiService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.service.UiElementsService;
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
import java.util.Optional;

@RestController
@RequestMapping("infraService")
@Api(value = "Get Information about a Service")
public class InfraServiceController {

    private static final Logger logger = LogManager.getLogger(InfraServiceController.class.getName());
    private final InfraServiceService<InfraService, InfraService> infraService;
    private final UiElementsService uiElementsService;

    @Autowired
    InfraServiceController(InfraServiceService<InfraService, InfraService> service,
                           UiElementsService uiElementsService) {
        this.infraService = service;
        this.uiElementsService = uiElementsService;
    }

    @DeleteMapping(path = {"{id}", "{id}/{version}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfraService> delete(@PathVariable("id") String id, @PathVariable Optional<String> version, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        InfraService service;
        if (version.isPresent())
            service = infraService.get(id, version.get());
        else
            service = infraService.get(id);
        if (service == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        infraService.delete(service);
        logger.info("User '{}' deleted InfraService '{}' with id: '{}'", authentication, service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = "delete/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfraService> deleteAll(@ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<InfraService> services = infraService.getAll(ff, null).getResults();
        for (InfraService service : services) {
            logger.info("Deleting service with name: {}", service.getService().getName());
            infraService.delete(service);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(path = {"updateFields/all"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<InfraService>> updateFields(InfraService service, Authentication authentication) {
        return new ResponseEntity<>(infraService.eInfraCentralUpdate(service), HttpStatus.OK);
    }


    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.get(id), HttpStatus.OK);
    }

    @GetMapping(path = "{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> get(@PathVariable("id") String id, @PathVariable("version") String version,
                                            Authentication auth) {
        InfraService ret = infraService.get(id, version);
        return new ResponseEntity<>(ret, ret != null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfraService> add(@RequestBody InfraService service, Authentication authentication) {
        ResponseEntity<InfraService> ret = new ResponseEntity<>(infraService.add(service, authentication), HttpStatus.OK);
        logger.info("User '{}' added InfraService '{}' with id: {} and version: {}", authentication, service.getService().getName(), service.getService().getId(), service.getService().getVersion());
        logger.info(" Service Organisation: {}", service.getService().getResourceOrganisation());
        return ret;
    }

    // TODO: move elsewhere ??
    @GetMapping(path = "dynamic/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public UiService getEntry(@PathVariable("id") String id, Authentication authentication) {

        InfraService service = this.infraService.get(id);
        logger.info(service);

        return uiElementsService.createUiService(service);
    }

    // TODO: move elsewhere ??
    @PostMapping(path = "dynamic", produces = {MediaType.APPLICATION_JSON_VALUE})
    public UiService addDynamic(@RequestBody UiService service, Authentication authentication) {

        logger.info(service);
        InfraService infra = uiElementsService.createService(service);
        infra = infraService.addService(infra, authentication);

        return uiElementsService.createUiService(infra);
    }

    // TODO: move elsewhere ??
    @PutMapping(path = "dynamic", produces = {MediaType.APPLICATION_JSON_VALUE})
    public UiService putDynamic(@RequestBody UiService service, Authentication authentication) throws ResourceNotFoundException {

        logger.info(service);
        InfraService infra = uiElementsService.createService(service);
        if (infra.getId() == null) {
            return addDynamic(service, authentication);
        }

        InfraService previous = infraService.get(infra.getId());
        previous.setService(infra.getService());
        previous.setExtras(infra.getExtras());
        infra = infraService.updateService(previous, authentication);

        return uiElementsService.createUiService(infra);
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfraService> update(@RequestBody InfraService service, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<InfraService> ret = new ResponseEntity<>(infraService.update(service, authentication), HttpStatus.OK);
        logger.info("User '{}' updated InfraService '{}' with id: {}", authentication, service.getService().getName(), service.getService().getId());
        return ret;
    }

    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody InfraService service, @ApiIgnore Authentication auth) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(infraService.validate(service));
        logger.info("Validating InfraService: {}", service.getService().getName());
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
    public ResponseEntity<Paging<InfraService>> getAll(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        return ResponseEntity.ok(infraService.getAll(ff, authentication));
    }

    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<InfraService>>> getBy(@PathVariable String field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        return ResponseEntity.ok(infraService.getBy(field));
    }

    @PatchMapping(path = "publish/{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfraService> setActive(@PathVariable String id, @PathVariable String version,
                                                  @RequestParam boolean active, @RequestParam Boolean latest,
                                                  @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service = infraService.get(id, version);
        service.setActive(active);
        service.setLatest(latest);
        Metadata metadata = service.getMetadata();
        metadata.setModifiedBy("system");
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        service.setMetadata(metadata);
        if (active) {
            logger.info("User '{}' set InfraService '{}' with id: {} as active", auth.getName(), service.getService().getName(), service.getService().getId());
        } else {
            logger.info("User '{}' set InfraService '{}' with id: {} as inactive", auth.getName(), service.getService().getName(), service.getService().getId());
        }
        return ResponseEntity.ok(infraService.update(service, auth));
    }

}
