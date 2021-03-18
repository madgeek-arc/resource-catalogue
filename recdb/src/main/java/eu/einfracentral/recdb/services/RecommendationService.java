package eu.einfracentral.recdb.services;

import org.springframework.security.core.Authentication;
import java.util.List;

public interface RecommendationService <U extends Authentication>{

    /**
     * Get information about Resource Recommendations
     *
     * @param authentication
     * @return
     */
    List<String> getRecommendationServices(U authentication);

}
