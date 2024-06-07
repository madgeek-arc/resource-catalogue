package gr.uoa.di.madgik.resourcecatalogue.recdb.controllers;

import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.recdb.services.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;

@RestController
@RequestMapping("recommendation")
@Tag(name = "Recommendation Controller", description = "Get information about Resource Recommendations")
public class RecommendationController {

    private static final Logger logger = LogManager.getLogger(RecommendationController.class);
    private final RecommendationService<Service, Authentication> recommendationService;

    @Autowired
    RecommendationController(RecommendationService<Service, Authentication> recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "Given a specific user email, returns Service Recommendations.")
    @GetMapping(path = "getRecommendationServices/{limit}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getRecommendationServices(@PathVariable("limit") int limit, @Parameter(hidden = true) Authentication auth) {
        logger.trace("Getting user recommended Services");
        return recommendationService.getRecommendedResources(limit, auth);
    }
}
