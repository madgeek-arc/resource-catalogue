package eu.einfracentral.recdb.services;

import eu.einfracentral.domain.RichService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface RecommendationService <U extends Authentication>{

    /**
     * Get information about Resource Recommendations
     *
     * @param authentication
     * @return
     */
    ResponseEntity<List<RichService>> getRecommendationServices(int limit, U authentication);

}
