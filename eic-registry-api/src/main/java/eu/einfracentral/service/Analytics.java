package eu.einfracentral.service;

import java.util.Map;

public interface Analytics {

    /**
     * Get Visits for a specific label
     *
     * @param label Label
     * @param by    interval (Day, Week, Month, Year)
     * @return {@link Map}&lt;{@link String},{@link Integer}&gt;
     */
    Map<String, Integer> getVisitsForLabel(String label, StatisticsService.Interval by);

    /**
     * Get Visits for all Services
     *
     * @return {@link Map}&lt;{@link String},{@link Integer}&gt;
     */
    Map<String, Integer> getAllServiceVisits();
}
