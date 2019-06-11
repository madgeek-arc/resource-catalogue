package eu.einfracentral.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static eu.einfracentral.config.CacheConfig.CACHE_VISITS;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class AnalyticsService {

    private static final Logger logger = LogManager.getLogger(AnalyticsService.class);
    private static final String visitsTemplate = "%s/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=%s&period=day&flat=1&filter_limit=100&period=%s&date=last30";
    private static final String serviceVisitsTemplate = "%s/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=%s&flat=1&period=range&date=2017-01-01,%s";
    private String visits;
    private String serviceVisits;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    @Value("${matomoHost:localhost}")
    private String matomoHost;

    @Value("${matomoToken:}")
    private String matomoToken;

    @Value("${matomoSiteId:1}")
    private String matomoSiteId;

    @Value("${matomoAuthorizationHeader:}")
    private String authorizationHeader;

    @PostConstruct
    void postConstruct() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.add("Authorization", authorizationHeader);
        visits = String.format(visitsTemplate, matomoHost, matomoToken, matomoSiteId, "%s");
        serviceVisits = String.format(serviceVisitsTemplate, matomoHost, matomoToken, matomoSiteId, "%s");
    }

    @Cacheable(value = CACHE_VISITS)
    public Map<String, Integer> getAllServiceVisits() {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        JsonNode json = parse(getMatomoResponse(String.format(serviceVisits, date)));
        if (json != null) {
            try {
                Spliterators.spliteratorUnknownSize(json.iterator(), Spliterator.NONNULL);
                Map<String, Integer> results = new HashMap<>();
                for (JsonNode node : json) {
                    String[] labelValues = node.path("label").textValue().split("/service/");
                    if (labelValues.length == 2) {
                        results.putIfAbsent(labelValues[1], node.path("nb_visits").asInt(0));
                    }
                }
                return results;
            } catch (Exception e) {
                logger.error("Cannot find visits for the specific Service.", e);
            }
        }
        return new HashMap<>();
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
        } catch (Exception e) {
            logger.debug("Cannot find visits for the specific Service.", e);
        }
        return new HashMap<>();
    }

    private JsonNode getAnalyticsForLabel(String label, StatisticsService.Interval by) {
        return parse(getMatomoResponse(String.format(visits, by.getKey()) + "&label=" + label));
    }

    private static JsonNode parse(String json) {
        try {
            return new ObjectMapper(new JsonFactory()).readTree(json);
        } catch (IOException e) {
            logger.error("ERROR", e);
        }
        return null;
    }

    private String getMatomoResponse(String url) {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class, headers);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error(String.format("Could not get analytics from matomo%nResponse Code: %s%nResponse Body: %s",
                    responseEntity.getStatusCode().toString(), responseEntity.getBody()));
            throw new RuntimeException(responseEntity.getBody());
        }
        return responseEntity.getBody();
    }
}