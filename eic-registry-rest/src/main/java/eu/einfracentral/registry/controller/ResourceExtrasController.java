package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.EOSCIFGuidelines;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.ResourceExtras;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
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

    private final ResourceBundleService<ServiceBundle> resourceBundleService;

    @Autowired
    private final ResourceService<ServiceBundle, Authentication> publicResourceManager;

    public ResourceExtrasController(ResourceBundleService<ServiceBundle> resourceBundleService,
                                    @Qualifier("publicResourceManager") ResourceService<ServiceBundle, Authentication> publicResourceManager) {
        this.resourceBundleService = resourceBundleService;
        this.publicResourceManager = publicResourceManager;
    }

    @ApiOperation(value = "Add a new EOSC Interoperability Framework Guideline on a specific Resource")
    @PutMapping(path = "/add/eoscIFGuideline", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> addEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                            @RequestParam String pid, @RequestParam String label,
                                                            @RequestParam URL url, @RequestParam String semanticRelationship,
                                                            @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        // check PID uniqueness
        List<EOSCIFGuidelines> existingEoscIFGuidelines = serviceBundle.getResourceExtras().getEoscIFGuidelines();
        for (EOSCIFGuidelines guideline : existingEoscIFGuidelines){
            if (guideline.getPid().equals(pid)){
                throw new ValidationException(String.format("There is already an EOSC IF Guideline with the same PID " +
                        "registered for specific Resource - [%s]", serviceId));
            }
        }
        EOSCIFGuidelines eoscIFGuideline = new EOSCIFGuidelines(pid, label, url, semanticRelationship);
        blockUpdateIfResourceIsPublished(serviceBundle);
        ResourceExtras resourceExtras = serviceBundle.getResourceExtras();
        List<EOSCIFGuidelines> newEoscIFGuidenlines = new ArrayList<>();
        if (resourceExtras == null){
            ResourceExtras newResourceExtras = new ResourceExtras();
            newEoscIFGuidenlines.add(eoscIFGuideline);
            newResourceExtras.setEoscIFGuidelines(newEoscIFGuidenlines);
            serviceBundle.setResourceExtras(newResourceExtras);
        } else{
            List<EOSCIFGuidelines> oldEoscIFGuidenlines = resourceExtras.getEoscIFGuidelines();
            if (oldEoscIFGuidenlines == null || oldEoscIFGuidenlines.isEmpty()){
                newEoscIFGuidenlines.add(eoscIFGuideline);
                serviceBundle.getResourceExtras().setEoscIFGuidelines(newEoscIFGuidenlines);
            } else{
                oldEoscIFGuidenlines.add(eoscIFGuideline);
                serviceBundle.getResourceExtras().setEoscIFGuidelines(oldEoscIFGuidenlines);
            }
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] added a new eoscIFGuideline on the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, eoscIFGuideline));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Add a new Research Category on a specific Resource")
    @PutMapping(path = "/add/researchCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> addResearchCategory(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                             @RequestParam String researchCategory,
                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        ResourceExtras resourceExtras = serviceBundle.getResourceExtras();
        List<String> newResearchCategories = new ArrayList<>();
        if (resourceExtras == null){
            ResourceExtras newResourceExtras = new ResourceExtras();
            newResearchCategories.add(researchCategory);
            newResourceExtras.setResearchCategories(newResearchCategories);
            serviceBundle.setResourceExtras(newResourceExtras);
        } else{
            List<String> oldResearchCategories = resourceExtras.getResearchCategories();
            if (oldResearchCategories == null || oldResearchCategories.isEmpty()){
                newResearchCategories.add(researchCategory);
                serviceBundle.getResourceExtras().setResearchCategories(newResearchCategories);
            } else{
                oldResearchCategories.add(researchCategory);
                serviceBundle.getResourceExtras().setResearchCategories(oldResearchCategories);
            }
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] added a new researchCategory on the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, researchCategory));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's EOSC Interoperability Framework Guidelines given its ID")
    @PutMapping(path = "/update/eoscIFGuideline", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> updateEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                               @RequestBody String pid, @RequestParam(required = false) String label,
                                                               @RequestParam(required = false) URL url,
                                                               @RequestParam(required = false) String semanticRelationship,
                                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        ResourceExtras resourceExtras = serviceBundle.getResourceExtras();
        boolean found = false;
        if (resourceExtras == null){
            throw new ValidationException(String.format("Resource with id [%s] has no Resource Extras.", serviceId));
        } else{
            List<EOSCIFGuidelines> eoscIFGuidenlines = resourceExtras.getEoscIFGuidelines();
            if (eoscIFGuidenlines == null || eoscIFGuidenlines.isEmpty()){
                throw new ValidationException(String.format("Resource with id [%s] has no EOSC IF Guidelines.", serviceId));
            } else{
                for (EOSCIFGuidelines guideline : eoscIFGuidenlines){
                    if (guideline.getPid().equals(pid)){
                        found = true;
                        if (label != null && !label.equals("")){
                            guideline.setLabel(label);
                        }
                        if (url != null && !url.equals("")){
                            guideline.setUrl(url);
                        }
                        if (semanticRelationship != null && !semanticRelationship.equals("")){
                            guideline.setSemanticRelationship(semanticRelationship);
                        }
                    }
                }
                if (!found){
                    throw new ValidationException(String.format("Resource with id [%s] has no EOSC IF Guideline with PID [%s]", serviceId, pid));
                }
                serviceBundle.getResourceExtras().setEoscIFGuidelines(eoscIFGuidenlines);
            }
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] updated field eoscIFGuideline of the Resource [%s] with PID [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, pid));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Update a specific Resource's Research Categories field")
    @PutMapping(path = "/update/researchCategories", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> updateResearchCategories(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                  @RequestBody List<String> researchCategories,
                                                                  @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        ResourceExtras resourceExtras = serviceBundle.getResourceExtras();
        List<String> newResearchCategories = new ArrayList<>();
        if (resourceExtras == null){
            ResourceExtras newResourceExtras = new ResourceExtras();
            newResearchCategories.addAll(researchCategories);
            newResourceExtras.setResearchCategories(newResearchCategories);
            serviceBundle.setResourceExtras(newResourceExtras);
        } else{
            List<String> oldResearchCategories = resourceExtras.getResearchCategories();
            if (oldResearchCategories == null || oldResearchCategories.isEmpty()){
                newResearchCategories.addAll(researchCategories);
                serviceBundle.getResourceExtras().setResearchCategories(newResearchCategories);
            } else{
                oldResearchCategories.addAll(researchCategories);
                serviceBundle.getResourceExtras().setResearchCategories(oldResearchCategories);
            }
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] updated field researchCategories of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, researchCategories));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Update Service's Horizontal Service field")
    @PutMapping(path = "/update/horizontalService", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> updateHorizontalService(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                 @RequestParam boolean horizontalService,
                                                                 @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        ResourceExtras resourceExtras = serviceBundle.getResourceExtras();
        if (resourceExtras == null){
            ResourceExtras newResourceExtras = new ResourceExtras();
            newResourceExtras.setHorizontalService(horizontalService);
            serviceBundle.setResourceExtras(newResourceExtras);
        } else{
            resourceExtras.setHorizontalService(horizontalService);
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] updated the field horizontalService of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, horizontalService));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes an existing Interoperability Framework Guideline of a specific Resource")
    @PutMapping(path = "/delete/eoscIFGuideline", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> deleteEOSCIFGuideline(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                               @RequestParam String pid,
                                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        List<EOSCIFGuidelines> existingEOSCIFGuidelines = serviceBundle.getResourceExtras().getEoscIFGuidelines();
        if (existingEOSCIFGuidelines != null && !existingEOSCIFGuidelines.isEmpty()){
            existingEOSCIFGuidelines.removeIf(existingEOSCIFGuideline -> existingEOSCIFGuideline.getPid().equals(pid));
            serviceBundle.getResourceExtras().setEoscIFGuidelines(existingEOSCIFGuidelines);
        } else{
            throw new NullPointerException(String.format("The Resource [%s] has no EOSC IF Guidelines registered.", serviceId));
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] deleted the researchCategory of the Resource [%s] with pid [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, pid));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes an existing Research Category of a specific Resource")
    @PutMapping(path = "/delete/researchCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> deleteResearchCategory(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                @RequestParam String researchCategory,
                                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        List<String> existingResourceCategories = serviceBundle.getResourceExtras().getResearchCategories();
        if (existingResourceCategories != null && !existingResourceCategories.isEmpty()){
            existingResourceCategories.removeIf(existingResourceCategory -> existingResourceCategory.equals(researchCategory));
            serviceBundle.getResourceExtras().setResearchCategories(existingResourceCategories);
        } else{
            throw new NullPointerException(String.format("The Resource [%s] has no EOSC IF Guidelines registered.", serviceId));
        }
        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] deleted the researchCategory of the Resource [%s] with value [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId, researchCategory));
        publicResourceManager.update(serviceBundle, auth);
        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }

    private void blockUpdateIfResourceIsPublished(ServiceBundle serviceBundle){
        if (serviceBundle.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
    }
}
