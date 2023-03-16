package eu.einfracentral.service;

import eu.einfracentral.domain.Event;
import eu.einfracentral.dto.MapValues;
import eu.einfracentral.dto.PlaceCount;
import eu.einfracentral.dto.Value;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface StatisticsService {

    /**
     * Get time series of addToProject for a service.
     *
     * @param serviceId
     * @param by
     * @return
     */
    Map<String, Integer> addToProject(String serviceId, Interval by);

    /**
     * Get time series of visits for a service.
     *
     * @param serviceId
     * @param by
     * @return
     */
    Map<String, Integer> visits(String serviceId, Interval by);

    /**
     * Get time series of aggregate favourites for all services offered by a provider.
     *
     * @param providerId
     * @param by
     * @return
     */
    Map<String, Integer> providerAddToProject(String providerId, Interval by);

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
     * List of Place names and total number of Services offered by the specified Provider.
     *
     * @param providerId
     * @return
     */
    List<PlaceCount> servicesPerPlace(String providerId);

    /**
     * List of Place names and total number of Services offered by the specified Provider.
     *
     * @param providerId
     * @return
     */
    List<Value> servicesByPlace(String providerId, String place);

    /**
     * Providing the Provider's id, get the relation between all his services and their respective countries.
     *
     * @param id
     * @return
     */
    List<MapValues> mapServicesToGeographicalAvailability(String id);

    /**
     * Get the relation between all the Services and their Coordinating Country.
     *
     * @return
     */
    List<MapValues> mapServicesToProviderCountry();

    /**
     * Providing the Provider's id, get the relation between all his services and a specific Vocabulary (e.g. subcategories).
     *
     * @param id
     * @param vocabulary
     * @return
     */
    List<MapValues> mapServicesToVocabulary(String id, Vocabulary vocabulary);

    enum Vocabulary {
        SUBCATEGORY("subcategories"),
        SCIENTIFIC_SUBDOMAIN("scientific_subdomains"),
        TARGET_USERS("target_users"),
        ACCESS_MODES("access_modes"),
        ACCESS_TYPES("access_types"),
        ORDER_TYPE("order_type");

        private final String vocabulary;

        Vocabulary(final String vocabulary) {
            this.vocabulary = vocabulary;
        }

        public String getKey() {
            return vocabulary;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Vocabulary fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Vocabulary.values())
                    .filter(v -> v.vocabulary.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown value: " + s + " ; Valid options: "
                            + Arrays.stream(values())
                            .map(Vocabulary::getKey)
                            .collect(Collectors.joining(", "))));
        }
    }
}
