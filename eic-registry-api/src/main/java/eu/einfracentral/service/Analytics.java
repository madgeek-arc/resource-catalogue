package eu.einfracentral.service;

import java.util.Map;

public interface Analytics {

    Map<String, Integer> getVisitsForLabel(String label, StatisticsService.Interval by);

    Map<String, Integer> getAllServiceVisits();
}
