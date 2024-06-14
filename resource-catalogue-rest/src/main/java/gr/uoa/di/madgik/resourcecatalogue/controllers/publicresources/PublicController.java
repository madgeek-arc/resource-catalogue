package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public", description = "General methods related to Public resources")
public class PublicController {

    private final GenericResourceService genericResourceService;

    @Autowired
    PublicController(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Get a list of Resources based on a set of ids.")
    @GetMapping(path = "public/resources/ids", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<?>> getSomeResources(@RequestParam("ids") String[] ids) {
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