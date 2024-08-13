package gr.uoa.di.madgik.resourcecatalogue.service;


import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.PlaceCount;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface StatisticsService {

    /**
     * Get time series of addToProject for a Service.
     *
     * @param serviceId Service ID
     * @param by        interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link String},{@link Integer}&gt;
     */
    Map<String, Integer> addToProject(String serviceId, Interval by);

    /**
     * @param serviceId Service ID
     * @param by        interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link String},{@link Integer}&gt;
     * @deprecated Get time series of visits for a Service.
     */
    @Deprecated(forRemoval = true)
    Map<String, Integer> visits(String serviceId, Interval by);

    /**
     * Get time series of aggregate favourites for all Services offered by a provider.
     *
     * @param providerId Provider ID
     * @param by         interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link String},{@link Integer}&gt;
     */
    Map<String, Integer> providerAddToProject(String providerId, Interval by);

    /**
     * @param providerId Provider ID
     * @param by         interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link String},{@link Integer}&gt;
     * @deprecated Get time series of aggregate visits for all Services offered by a Provider.
     */
    @Deprecated(forRemoval = true)
    Map<String, Integer> providerVisits(String providerId, Interval by);

    /**
     * @param providerId Provider ID
     * @param by         interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link String},{@link Float}&gt;
     * @deprecated Get visitation percentages of a Provider's Services for the specified interval.
     */
    @Deprecated(forRemoval = true)
    Map<String, Float> providerVisitation(String providerId, Interval by);

    /**
     * Get the time series of the specified Event type.
     *
     * @param type User actionType
     * @param from from
     * @param to   to
     * @param by   interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link DateTime},{@link Map}&lt;{@link String},{@link Long}&gt;&gt;
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
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link PlaceCount}&gt;
     */
    List<PlaceCount> servicesPerPlace(String providerId);

    /**
     * List of Place names and total number of Services offered by the specified Provider.
     *
     * @param providerId Provider ID
     * @param place      Place
     * @return {@link List}&lt;{@link Value}&gt;
     */
    List<Value> servicesByPlace(String providerId, String place);

    /**
     * Providing the Provider's ID, get the relation between all his Services and their respective countries.
     *
     * @param id Provider ID
     * @return {@link List}&lt;{@link MapValues}&gt;
     */
    List<MapValues> mapServicesToGeographicalAvailability(String id);

    /**
     * Get the relation between all the Services and their Coordinating Country.
     *
     * @return {@link List}&lt;{@link MapValues}&gt;
     */
    List<MapValues> mapServicesToProviderCountry();

    /**
     * Providing the Provider's ID, get the relation between all his Services and a specific Vocabulary
     * (e.g. subcategories).
     *
     * @param id         Provider ID
     * @param vocabulary Vocabulary
     * @return {@link List}&lt;{@link MapValues}&gt;
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
