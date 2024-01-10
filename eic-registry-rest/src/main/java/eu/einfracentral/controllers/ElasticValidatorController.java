package eu.einfracentral.controllers;

import eu.einfracentral.service.ElasticValidatorService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("elasticValidator")
public class ElasticValidatorController {

    private final ElasticValidatorService elasticValidatorService;

    public ElasticValidatorController(ElasticValidatorService elasticValidatorService) {
        this.elasticValidatorService = elasticValidatorService;
    }

    @ApiIgnore
    @Secured("ROLE_ADMIN")
    @PostMapping(value = "validateResourceExistence")
    public void validateResourceExistence(@RequestParam String resourceType) {
        elasticValidatorService.validate(resourceType);
    }
}
