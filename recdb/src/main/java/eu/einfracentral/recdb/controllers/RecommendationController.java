package eu.einfracentral.recdb.controllers;

import eu.einfracentral.recdb.services.RecommendationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequestMapping("recommendation")
@Api(value = "Get information about Resource Recommendations")
public class RecommendationController {

    private static final Logger logger = LogManager.getLogger(RecommendationController.class);
    private RecommendationService recommendationService;

    @Autowired
    RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @ApiOperation(value = "Given a specific user email, returns all Service Recommendations.")
    @GetMapping(path = "getRecommendationServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getRecommendationServices(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return recommendationService.getRecommendationServices(auth);
    }
}
