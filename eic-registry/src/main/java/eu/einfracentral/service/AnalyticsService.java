package eu.einfracentral.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class AnalyticsService {

    private static final Logger logger = LogManager.getLogger(AnalyticsService.class);
    private static final String base = "%s/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=%s&period=day&flat=1&filter_limit=100&period=%s&label=%s&date=last30";
    private String visits;

    @Value("${matomoHost:localhost}")
    private String matomoHost;

    @Value("${matomoToken:}")
    private String matomoToken;

    @Value("${matomoSiteId:1}")
    private String matomoSiteId;

    @PostConstruct
    void postConstruct() {
        visits = String.format(base, matomoHost, matomoToken, matomoSiteId, "%s", "%s", "%s");
    }

    public Map<String, Integer> getVisitsForLabel(String label) {
        try {
            Map<String, Integer> results = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(getAnalyticsForLabel(label, StatisticsService.Interval.YEAR).fields(), Spliterator.NONNULL), false).collect(
                    Collectors.toMap(
                            Map.Entry::getKey,
                            dayStats -> dayStats.getValue().get(0) != null ? dayStats.getValue().get(0).path("nb_visits").asInt(0) : 0
                    )
            );
            Map<String, Integer> sortedResults = new TreeMap<>(results);
            return sortedResults;
        } catch (Exception e){
            logger.error("Cannot find visits for the specific Service.", e);
        }
        return null;
    }

    public Map<String, Integer> getVisitsForLabel(String label, StatisticsService.Interval by) {
        try {
            Map<String, Integer> results = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(getAnalyticsForLabel(label, by).fields(), Spliterator.NONNULL), false).collect(
                    Collectors.toMap(
                            Map.Entry::getKey,
                            dayStats -> dayStats.getValue().get(0) != null ? dayStats.getValue().get(0).path("nb_visits").asInt(0) : 0
                    )
            );
            Map<String, Integer> sortedResults = new TreeMap<>(results);
            return sortedResults;
        } catch (Exception e){
            logger.error("Cannot find visits for the specific Service.", e);
        }
        return null;
    }

    private JsonNode getAnalyticsForLabel(String label, StatisticsService.Interval by) {
        return parse(getURL(String.format(visits, by.getKey(), label)));
    }

    private static JsonNode parse(String json) {
        try {
            return new ObjectMapper(new JsonFactory()).readTree(json);
        } catch (IOException e) {
            logger.error("ERROR", e);
        }
        return null;
    }

    private static String getURL(String url) {
        StringBuilder ret = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                ret.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            logger.error("ERROR", e);
        }
        return ret.toString();
    }
}