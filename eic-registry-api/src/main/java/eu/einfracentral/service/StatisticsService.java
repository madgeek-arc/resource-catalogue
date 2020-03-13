package eu.einfracentral.service;

import eu.einfracentral.domain.Event;
import eu.einfracentral.dto.MapValue;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface StatisticsService {

    /**
     * Get time series of ratings for a service.
     *
     * @param serviceId
     * @param by
     * @return
     */
    Map<String, Float> ratings(String serviceId, Interval by);

    /**
     * Get time series of favourites for a service.
     *
     * @param serviceId
     * @param by
     * @return
     */
    Map<String, Integer> favourites(String serviceId, Interval by);

    /**
     * Get time series of visits for a service.
     *
     * @param serviceId
     * @param by
     * @return
     */
    Map<String, Integer> visits(String serviceId, Interval by);

    /**
     * Get time series of aggregate ratings for all services offered by a provider.
     *
     * @param providerId
     * @param by
     * @return
     */
    Map<String, Float> providerRatings(String providerId, Interval by);

    /**
     * Get time series of aggregate favourites for all services offered by a provider.
     *
     * @param providerId
     * @param by
     * @return
     */
    Map<String, Integer> providerFavourites(String providerId, Interval by);

    /**
     * Get time series of aggregate visits for all services offered by a provider.
     *
     * @param providerId
     * @param by
     * @return
     */
    Map<String, Integer> providerVisits(String providerId, Interval by);

    /**
     * Get visitation percentages of a provider's services for the specified interval.
     *
     * @param providerId
     * @param by
     * @return
     */
    Map<String, Float> providerVisitation(String providerId, Interval by);

    /**
     * Get the time series of the specified Event type.
     *
     * @param type
     * @param from
     * @param to
     * @param by
     * @return
     */
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

    /**
     * Providing the Provider's id, get the relation between all his services and their respective countries
     *
     * @param id
     * @return
     */
    List<MapValue> providerServiceGeographicalAvailability(String id);
}
