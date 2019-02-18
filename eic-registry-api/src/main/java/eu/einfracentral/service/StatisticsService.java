package eu.einfracentral.service;

import eu.einfracentral.domain.Event;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

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

    Map<DateTime, Map<String, Long>> events(Event.UserActionType type, Date from, Date to, Interval by);

    enum Interval {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }
}