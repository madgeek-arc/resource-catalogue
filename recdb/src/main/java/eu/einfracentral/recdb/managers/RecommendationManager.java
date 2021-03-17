package eu.einfracentral.recdb.managers;

import eu.einfracentral.domain.User;
import eu.einfracentral.recdb.services.RecommendationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

import java.util.List;

@Component
public class RecommendationManager implements RecommendationService{

    private static final Logger logger = LogManager.getLogger(RecommendationManager.class);

    @Autowired
    public RecommendationManager() {
    }

//    public List<String> getRecommendationServices(Authentication authentication){
//        String userEmail = User.of(authentication).getEmail();
//    }

}
