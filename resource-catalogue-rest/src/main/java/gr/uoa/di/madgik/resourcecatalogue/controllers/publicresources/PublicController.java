package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@Api(value = "General methods related to Public Resources")
public class PublicController {

    private final GenericResourceService genericResourceService;

    @Autowired
    PublicController(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "public/resources/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<?>> getSomeResources(@PathVariable("ids") String[] ids) {
        String[] resourceTypeNames = new String[]{"service", "training_resource"};
        List<?> someResources = new ArrayList<>();
        for (String id : ids) {
            for (String resourceType : resourceTypeNames) {
                try {
                    someResources.add(genericResourceService.get(resourceType, id));
                } catch (ResourceNotFoundException e) {
                }
            }
        }
        List<?> ret = someResources.stream().map(r -> ((Bundle<?>) r).getPayload()).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

}