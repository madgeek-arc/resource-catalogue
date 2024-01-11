package eu.einfracentral.controllers;

import eu.einfracentral.service.ElasticDBValidatorService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("elasticDBValidator")
public class ElasticDBValidatorController {

    private final ElasticDBValidatorService elasticDBValidatorService;

    public ElasticDBValidatorController(ElasticDBValidatorService elasticDBValidatorService) {
        this.elasticDBValidatorService = elasticDBValidatorService;
    }

    @ApiIgnore
    @Secured("ROLE_ADMIN")
    @PostMapping(value = "validateResourceExistence")
    public void validateResourceExistence(@RequestParam String resourceType,
                                          @RequestParam boolean validateDBtoElastic) {
        elasticDBValidatorService.validate(resourceType, validateDBtoElastic);
    }
}
