package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.registry.service.ResourceBundleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@RestController
@RequestMapping("resource-extras")
//@Api(value = "Modify a Resource's extra info")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
public class ResourceExtrasController {

    private static final Logger logger = LogManager.getLogger(ResourceExtrasController.class);

    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;

    public ResourceExtrasController(ResourceBundleService<ServiceBundle> serviceBundleService,
                                    ResourceBundleService<DatasourceBundle> datasourceBundleService) {
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
    }

    @ApiOperation(value = "Update a specific Resource's EOSC Interoperability Framework Guidelines given its ID")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceBundle<?>> updateEOSCIFGuidelines(@RequestParam String resourceId, @RequestParam String catalogueId,
                                                    @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines, @RequestParam String type,
                                                    @ApiIgnore Authentication auth) {
        ResourceBundle<?> bundle;
        switch (type){
            case "service":
                bundle = serviceBundleService.updateEOSCIFGuidelines(resourceId, catalogueId, eoscIFGuidelines, auth);
                break;
            case "datasource":
                bundle = datasourceBundleService.updateEOSCIFGuidelines(resourceId, catalogueId, eoscIFGuidelines, auth);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Type [%s] is not supported.", type));
        }
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's Research Categories field")
    @PutMapping(path = "/update/researchCategories", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceBundle<?>> updateResearchCategories(@RequestParam String resourceId, @RequestParam String catalogueId,
                                                                  @RequestBody List<String> researchCategories, @RequestParam String type,
                                                                  @ApiIgnore Authentication auth) {
        ResourceBundle<?> bundle;
        switch (type){
            case "service":
                bundle = serviceBundleService.updateResearchCategories(resourceId, catalogueId, researchCategories, auth);
                break;
            case "datasource":
                bundle = datasourceBundleService.updateResearchCategories(resourceId, catalogueId, researchCategories, auth);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Type [%s] is not supported.", type));
        }
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Update Resource's Horizontal Service field")
    @PutMapping(path = "/update/horizontalService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceBundle<?>> updateHorizontalService(@RequestParam String resourceId, @RequestParam String catalogueId,
                                                                 @RequestParam boolean horizontalService, @RequestParam String type,
                                                                 @ApiIgnore Authentication auth) {
        ResourceBundle<?> bundle;
        switch (type){
            case "service":
                bundle = serviceBundleService.updateHorizontalService(resourceId, catalogueId, horizontalService, auth);
                break;
            case "datasource":
                bundle = datasourceBundleService.updateHorizontalService(resourceId, catalogueId, horizontalService, auth);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Type [%s] is not supported.", type));
        }
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }
}
