package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.EOSCIFGuidelines;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.InfraServiceExtras;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ResourceService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("resource-extras")
@Api(value = "Modify a Service's extra info")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
public class ResourceExtrasController {

    private static final Logger logger = LogManager.getLogger(ResourceExtrasController.class);

    @Autowired
    private JmsTemplate jmsTopicTemplate;

    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    private final ResourceService<InfraService, Authentication> publicResourceManager;

    public ResourceExtrasController(InfraServiceService<InfraService, InfraService> infraServiceService,
                                    @Qualifier("publicResourceManager") ResourceService<InfraService, Authentication> publicResourceManager) {
        this.infraServiceService = infraServiceService;
        this.publicResourceManager = publicResourceManager;
    }

    @ApiOperation(value = "Add a new EOSC Interoperability Framework Guideline on a specific Resource")
    @PutMapping(path = "/add/eoscIFGuideline", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> addEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                           @RequestParam(required = false) String pid, @RequestParam(required = false) String label,
                                                           @RequestParam(required = false) URL url, @RequestParam(required = false) String semanticRelationship,
                                                           @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        EOSCIFGuidelines eoscIFGuideline = new EOSCIFGuidelines(pid, label, url, semanticRelationship);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        InfraServiceExtras infraServiceExtras = infraService.getResourceExtras();
        List<EOSCIFGuidelines> newEoscIFGuidenlines = new ArrayList<>();
        if (infraServiceExtras == null){
            InfraServiceExtras newInfraServiceExtras = new InfraServiceExtras();
            newEoscIFGuidenlines.add(eoscIFGuideline);
            newInfraServiceExtras.setEoscIFGuidelines(newEoscIFGuidenlines);
            infraService.setResourceExtras(newInfraServiceExtras);
        } else{
            List<EOSCIFGuidelines> oldEoscIFGuidenlines = infraServiceExtras.getEoscIFGuidelines();
            if (oldEoscIFGuidenlines == null || oldEoscIFGuidenlines.isEmpty()){
                newEoscIFGuidenlines.add(eoscIFGuideline);
                infraService.getResourceExtras().setEoscIFGuidelines(newEoscIFGuidenlines);
            } else{
                oldEoscIFGuidenlines.add(eoscIFGuideline);
                infraService.getResourceExtras().setEoscIFGuidelines(oldEoscIFGuidenlines);
            }
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] added a new eoscIFGuideline on the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, eoscIFGuideline));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Add a new Research Category on a specific Resource")
    @PutMapping(path = "/add/researchCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> addResearchCategory(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                            @RequestParam String researchCategory,
                                                            @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        InfraServiceExtras infraServiceExtras = infraService.getResourceExtras();
        List<String> newResearchCategories = new ArrayList<>();
        if (infraServiceExtras == null){
            InfraServiceExtras newInfraServiceExtras = new InfraServiceExtras();
            newResearchCategories.add(researchCategory);
            newInfraServiceExtras.setResearchCategories(newResearchCategories);
            infraService.setResourceExtras(newInfraServiceExtras);
        } else{
            List<String> oldResearchCategories = infraServiceExtras.getResearchCategories();
            if (oldResearchCategories == null || oldResearchCategories.isEmpty()){
                newResearchCategories.add(researchCategory);
                infraService.getResourceExtras().setResearchCategories(newResearchCategories);
            } else{
                oldResearchCategories.add(researchCategory);
                infraService.getResourceExtras().setResearchCategories(oldResearchCategories);
            }
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] added a new researchCategory on the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, researchCategory));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's EOSC Interoperability Framework Guideline field")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> updateEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                              @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines,
                                                              @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        InfraServiceExtras infraServiceExtras = infraService.getResourceExtras();
        List<EOSCIFGuidelines> newEoscIFGuidenlines = new ArrayList<>();
        if (infraServiceExtras == null){
            InfraServiceExtras newInfraServiceExtras = new InfraServiceExtras();
            newEoscIFGuidenlines.addAll(eoscIFGuidelines);
            newInfraServiceExtras.setEoscIFGuidelines(newEoscIFGuidenlines);
            infraService.setResourceExtras(newInfraServiceExtras);
        } else{
            List<EOSCIFGuidelines> oldEoscIFGuidenlines = infraServiceExtras.getEoscIFGuidelines();
            if (oldEoscIFGuidenlines == null || oldEoscIFGuidenlines.isEmpty()){
                newEoscIFGuidenlines.addAll(eoscIFGuidelines);
                infraService.getResourceExtras().setEoscIFGuidelines(newEoscIFGuidenlines);
            } else{
                oldEoscIFGuidenlines.addAll(eoscIFGuidelines);
                infraService.getResourceExtras().setEoscIFGuidelines(oldEoscIFGuidenlines);
            }
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] updated field eoscIFGuidelines of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, eoscIFGuidelines));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's Research Categories field")
    @PutMapping(path = "/update/researchCategories", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> updateResearchCategories(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                 @RequestBody List<String> researchCategories,
                                                                 @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        InfraServiceExtras infraServiceExtras = infraService.getResourceExtras();
        List<String> newResearchCategories = new ArrayList<>();
        if (infraServiceExtras == null){
            InfraServiceExtras newInfraServiceExtras = new InfraServiceExtras();
            newResearchCategories.addAll(researchCategories);
            newInfraServiceExtras.setResearchCategories(newResearchCategories);
            infraService.setResourceExtras(newInfraServiceExtras);
        } else{
            List<String> oldResearchCategories = infraServiceExtras.getResearchCategories();
            if (oldResearchCategories == null || oldResearchCategories.isEmpty()){
                newResearchCategories.addAll(researchCategories);
                infraService.getResourceExtras().setResearchCategories(newResearchCategories);
            } else{
                oldResearchCategories.addAll(researchCategories);
                infraService.getResourceExtras().setResearchCategories(oldResearchCategories);
            }
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] updated field researchCategories of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, researchCategories));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Update Service's Horizontal Service field")
    @PutMapping(path = "/update/horizontalService", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> updateHorizontalService(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                @RequestParam boolean horizontalService,
                                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        InfraServiceExtras infraServiceExtras = infraService.getResourceExtras();
        if (infraServiceExtras == null){
            InfraServiceExtras newInfraServiceExtras = new InfraServiceExtras();
            newInfraServiceExtras.setHorizontalService(horizontalService);
            infraService.setResourceExtras(newInfraServiceExtras);
        } else{
            infraServiceExtras.setHorizontalService(horizontalService);
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] updated the field horizontalService of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, horizontalService));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes an existing Interoperability Framework Guideline of a specific Resource")
    @PutMapping(path = "/delete/eoscIFGuideline", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> deleteEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                               @RequestParam String pid,
                                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        List<EOSCIFGuidelines> existingEOSCIFGuidelines = infraService.getResourceExtras().getEoscIFGuidelines();
        if (existingEOSCIFGuidelines != null && !existingEOSCIFGuidelines.isEmpty()){
            existingEOSCIFGuidelines.removeIf(existingEOSCIFGuideline -> existingEOSCIFGuideline.getPid().equals(pid));
        } else{
            throw new NullPointerException(String.format("The Resource [%s] has no EOSC IF Guidelines registered.", serviceId));
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] deleted the researchCategory of the Resource [%s] with pid [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, pid));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes an existing Research Category of a specific Resource")
    @PutMapping(path = "/delete/researchCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InfraService> deleteResearchCategory(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                            @RequestParam String researchCategory,
                                                            @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService infraService = infraServiceService.get(serviceId, catalogueId);
        // block update if Service is published (Public API)
        if (infraService.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
        List<String> existingResourceCategories = infraService.getResourceExtras().getResearchCategories();
        if (existingResourceCategories != null && !existingResourceCategories.isEmpty()){
            existingResourceCategories.removeIf(existingResourceCategory -> existingResourceCategory.equals(researchCategory));
        } else{
            throw new NullPointerException(String.format("The Resource [%s] has no EOSC IF Guidelines registered.", serviceId));
        }
        infraServiceService.validate(infraService);
        infraServiceService.update(infraService, auth);
        logger.info(String.format("User [%s]-[%s] deleted the researchCategory of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, researchCategory));
        publicResourceManager.update(infraService, auth);
        jmsTopicTemplate.convertAndSend("resource.update", infraService);
        return new ResponseEntity<>(infraService, HttpStatus.OK);
    }
}
