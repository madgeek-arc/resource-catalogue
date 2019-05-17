package eu.einfracentral.service;

import eu.einfracentral.domain.Event;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public interface StatisticsService {
    Map<String, Float> ratings(String id, Interval by);
    Map<String, Integer> externals(String id);
    Map<String, Integer> internals(String id);
    Map<String, Integer> favourites(String id, Interval by);
    Map<String, Integer> visits(String id, Interval by);
    Map<String, Float> pRatings(String id, Interval by);
    Map<String, Integer> pExternals(String id);
    Map<String, Integer> pInternals(String id);
    Map<String, Integer> pFavourites(String id, Interval by);
    Map<String, Integer> pVisits(String id, Interval by);
    Map<String, Float> pVisitation(String id, Interval by);

    Map<DateTime, Map<String, Long>> events(Event.UserActionType type, Date from, Date to, Interval by);

    enum Interval {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year");

        private final String interval;

        Interval(final String interval) {
            this.interval = interval;
        }

        public String getKey() {
            return interval;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Interval fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Interval.values())
                    .filter(v -> v.interval.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown value: " + s + " ; Valid options: "
                            + Arrays.stream(values())
                            .map(Interval::getKey)
                            .collect(Collectors.joining(", "))));
        }

    }
}