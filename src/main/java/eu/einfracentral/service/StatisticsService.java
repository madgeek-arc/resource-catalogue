package eu.einfracentral.service;

import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 23/04/18.
 */
@Service("statisticsService")
public interface StatisticsService {
    Map<String, Float> ratings(String id);
    Map<String, Integer> externals(String id);
    Map<String, Integer> internals(String id);
    Map<String, Integer> favourites(String id);
    Map<String, Integer> visits(String id);
    Map<String, Float> pRatings(String id);
    Map<String, Integer> pExternals(String id);
    Map<String, Integer> pInternals(String id);
    Map<String, Integer> pFavourites(String id);
    Map<String, Integer> pVisits(String id);
    Map<String, Float> pVisitation(String id);
}