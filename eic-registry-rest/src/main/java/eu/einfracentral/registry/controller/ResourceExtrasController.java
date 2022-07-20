package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.EOSCIFGuidelines;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("service-extras")
@Api(value = "Modify a Service's extra info")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
public class ServiceExtrasController {

    private static final Logger logger = LogManager.getLogger(ServiceExtrasController.class);

    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    public ServiceExtrasController(InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.infraServiceService = infraServiceService;
    }

    @ApiOperation(value = "Add a new EOSC Interoperability Framework Guideline on a specific Resource")
    @PutMapping(path = "/add/eoscIFGuideline", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> addEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                           @RequestParam EOSCIFGuidelines eoscIFGuideline,
                                                           @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        List<EOSCIFGuidelines> oldEoscIFGuidenlines = infraService.getResourceExtras().getEoscIFGuidelines();
        if (oldEoscIFGuidenlines == null || oldEoscIFGuidenlines.isEmpty()){
            List<EOSCIFGuidelines> newEoscIFGuidenlines = new ArrayList<>();
            newEoscIFGuidenlines.add(eoscIFGuideline);
            infraService.getResourceExtras().setEoscIFGuidelines(newEoscIFGuidenlines);
        } else{
            oldEoscIFGuidenlines.add(eoscIFGuideline);
            infraService.getResourceExtras().setEoscIFGuidelines(oldEoscIFGuidenlines);
        }
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s] added a new eoscIFGuideline on the Resource [%s] with value [%s]",
                auth.getName(), serviceId, eoscIFGuideline));
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Add a new Research Category on a specific Resource")
    @PutMapping(path = "/add/researchCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> addResearchCategory(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                            @RequestParam String researchCategory,
                                                            @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        List<String> oldResearchCategories = infraService.getResourceExtras().getResearchCategories();
        if (oldResearchCategories == null || oldResearchCategories.isEmpty()){
            List<String> newResearchCategories = new ArrayList<>();
            newResearchCategories.add(researchCategory);
            infraService.getResourceExtras().setResearchCategories(newResearchCategories);
        } else{
            oldResearchCategories.add(researchCategory);
            infraService.getResourceExtras().setResearchCategories(oldResearchCategories);
        }
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s] added a new researchCategory on the Resource [%s] with value [%s]",
                auth.getName(), serviceId, researchCategory));
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's EOSC Interoperability Framework Guideline field")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> updateEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                              @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines,
                                                              @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        List<EOSCIFGuidelines> oldEoscIFGuidenlines = infraService.getResourceExtras().getEoscIFGuidelines();
        if (oldEoscIFGuidenlines == null || oldEoscIFGuidenlines.isEmpty()){
            infraService.getResourceExtras().setEoscIFGuidelines(eoscIFGuidelines);
        } else{
            oldEoscIFGuidenlines.addAll(eoscIFGuidelines);
            infraService.getResourceExtras().setEoscIFGuidelines(oldEoscIFGuidenlines);
        }
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s] updated field eoscIFGuidelines of the Resource [%s] with value [%s]",
                auth.getName(), serviceId, eoscIFGuidelines));
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's Research Categories field")
    @PutMapping(path = "/update/researchCategories", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> updateResearchCategories(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                 @RequestBody List<String> researchCategories,
                                                                 @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        List<String> oldResearchCategories = infraService.getResourceExtras().getResearchCategories();
        if (oldResearchCategories == null || oldResearchCategories.isEmpty()){
            infraService.getResourceExtras().setResearchCategories(researchCategories);
        } else{
            oldResearchCategories.addAll(researchCategories);
            infraService.getResourceExtras().setResearchCategories(oldResearchCategories);
        }
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s] updated field researchCategories of the Resource [%s] with value [%s]",
                auth.getName(), serviceId, researchCategories));
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Update Service's Horizontal Service field")
    @PutMapping(path = "/update/horizontalService", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> updateHorizontalService(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                @RequestParam boolean horizontalService,
                                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        infraService.getResourceExtras().setHorizontalService(horizontalService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s] updated the field horizontalService of the Resource [%s] with value [%s]",
                auth.getName(), serviceId, horizontalService));
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

}
