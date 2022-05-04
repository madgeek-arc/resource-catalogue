package eu.einfracentral.recdb.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface RecommendationService<T, U extends Authentication> {

    /**
     * Get a list of Resource Recommendations
     *
     * @param limit, authentication
     * @return List<T>
     */
    ResponseEntity<List<T>> getRecommendedResources(int limit, U authentication);

}
