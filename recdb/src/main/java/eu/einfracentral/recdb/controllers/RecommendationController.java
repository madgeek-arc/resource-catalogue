package eu.einfracentral.recdb.controllers;

import eu.einfracentral.domain.RichResource;
import eu.einfracentral.recdb.services.RecommendationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final RecommendationService<RichResource, Authentication> recommendationService;

    @Autowired
    RecommendationController(RecommendationService<RichResource, Authentication> recommendationService) {
        this.recommendationService = recommendationService;
    }

    @ApiOperation(value = "Given a specific user email, returns Service Recommendations.")
    @GetMapping(path = "getRecommendationServices/{limit}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RichResource>> getRecommendationServices(@PathVariable("limit") int limit, @ApiIgnore Authentication auth) {
        logger.trace("Getting user recommended Services");
        return recommendationService.getRecommendedResources(limit, auth);
    }
}
