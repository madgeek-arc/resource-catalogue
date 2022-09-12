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
import java.util.*;

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

    @ApiOperation(value = "Update a specific Resource's EOSC Interoperability Framework Guidelines given its ID")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ServiceBundle> updateEOSCIFGuidelines(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                               @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines,
                                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = resourceBundleService.get(serviceId, catalogueId);
        blockUpdateIfResourceIsPublished(serviceBundle);
        ResourceExtras resourceExtras = serviceBundle.getResourceExtras();
        if (resourceExtras == null){
            ResourceExtras newResourceExtras = new ResourceExtras();
            List<EOSCIFGuidelines> newEOSCIFGuidelines = new ArrayList<>(eoscIFGuidelines);
            newResourceExtras.setEoscIFGuidelines(newEOSCIFGuidelines);
            serviceBundle.setResourceExtras(newResourceExtras);
        } else{
            serviceBundle.getResourceExtras().setEoscIFGuidelines(eoscIFGuidelines);
        }
        // check PID consistency
        checkEOSCIFGuidelinesPIDConsistency(serviceBundle);

        resourceBundleService.validate(serviceBundle);
        resourceBundleService.update(serviceBundle, auth);
        logger.info(String.format("User [%s]-[%s] updated field eoscIFGuidelines of the Resource [%s]",
                User.of(auth).getFullName(), User.of(auth).getEmail(), serviceId));
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
                for (String researchCategory : researchCategories){
                    if (!oldResearchCategories.contains(researchCategory)){
                        oldResearchCategories.add(researchCategory);
                    }
                }
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

    private void blockUpdateIfResourceIsPublished(ServiceBundle serviceBundle){
        if (serviceBundle.getMetadata().isPublished()){
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
    }

    private void checkEOSCIFGuidelinesPIDConsistency(ServiceBundle serviceBundle){
        List<String> pidList = new ArrayList<>();
        for (EOSCIFGuidelines eoscIFGuideline : serviceBundle.getResourceExtras().getEoscIFGuidelines()){
            pidList.add(eoscIFGuideline.getPid());
        }
        Set<String> pidSet = new HashSet<String>(pidList);
        if(pidSet.size() < pidList.size()){
            throw new ValidationException("EOSCIFGuidelines cannot have duplicate PIDs.");
        }
    }
}
