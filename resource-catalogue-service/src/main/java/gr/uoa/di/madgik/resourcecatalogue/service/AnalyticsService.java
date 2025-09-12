/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_VISITS;

@Component
public class AnalyticsService implements Analytics {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String visitsTemplate = "%s/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=%s&period=day&flat=1&filter_limit=100&period=%s&date=last30";
    private static final String serviceVisitsTemplate = "%s/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=%s&flat=1&period=range&date=2017-01-01,%s";
    private String visits;
    private String serviceVisits;
    private WebClient webClient;
    private HttpHeaders headers;

    @Value("${matomo.host:localhost}")
    private String matomoHost;

    @Value("${matomo.token:}")
    private String matomoToken;

    @Value("${matomo.site-id:1}")
    private String matomoSiteId;

    @Value("${matomo.authorization.header:}")
    private String authorizationHeader;

    @Autowired
    private CacheManager cacheManager;

    @PostConstruct
    void postConstruct() {
        webClient = WebClient.builder().build();
        headers = new HttpHeaders();
        headers.add("Authorization", authorizationHeader);
        visits = String.format(visitsTemplate, matomoHost, matomoToken, matomoSiteId, "%s");
        serviceVisits = String.format(serviceVisitsTemplate, matomoHost, matomoToken, matomoSiteId, "%s");
    }

    /**
     * Scheduler that refreshes CACHE_VISITS every 5 minutes.
     *
     * @return
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void updateVisitsScheduler() {
        if (matomoHost == null || matomoHost.isBlank()) {
            logger.debug("Matomo host not configured. Skipping visit update.");
            return;
        }

        try {
            Map<String, Integer> visits = getServiceVisits();
            Cache cache = cacheManager.getCache(CACHE_VISITS);
            Objects.requireNonNull(cache).put(CACHE_VISITS, visits);
        } catch (Exception e) {
            logger.warn("Failed to update visits from Matomo: {}", e.getMessage());
        }
    }


    @Override
    public Map<String, Integer> getAllServiceVisits() {
        return getServiceVisits();
    }

    @Override
    public Map<String, Integer> getVisitsForLabel(String label, StatisticsService.Interval by) {
        try {
            Map<String, Integer> results = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(getAnalyticsForLabel(label, by).fields(), Spliterator.NONNULL), false).collect(
                    Collectors.toMap(
                            Map.Entry::getKey,
                            dayStats -> dayStats.getValue().get(0) != null ? dayStats.getValue().get(0).path("nb_visits").asInt(0) : 0
                    )
            );
            return new TreeMap<>(results);
        } catch (Exception e) {
            logger.warn("Cannot find visits for the label '{}'\n", label, e);
        }
        return new HashMap<>();
    }

    private Map<String, Integer> getServiceVisits() {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        JsonNode json = parse(getMatomoResponse(String.format(serviceVisits, date)).block());
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
                logger.error("Cannot retrieve visits for all Services\nMatomo response: {}\n", json, e);
            }
        }
        return new HashMap<>();
    }

    private JsonNode getAnalyticsForLabel(String label, StatisticsService.Interval by) {
        return parse(getMatomoResponse(String.format(visits, by.getKey()) + "&label=" + label).block());
    }

    private static JsonNode parse(String json) {
        try {
            return new ObjectMapper(new JsonFactory()).readTree(json);
        } catch (JsonParseException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private Mono<String> getMatomoResponse(String url) {
        return webClient.get()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers)) // assuming 'headers' is an HttpHeaders object
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("No response body")
                            .flatMap(body -> {
                                logger.error("Could not retrieve analytics from Matomo\nResponse Code: {}\nResponse Body: {}",
                                        clientResponse.statusCode(), body);
                                return Mono.error(new RuntimeException("Failed request"));
                            });
                })
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    if (e instanceof IllegalArgumentException) {
                        logger.info("URI is not absolute");
                    } else {
                        logger.error("Could not retrieve analytics from Matomo", e);
                    }
                    return Mono.just(""); // fallback
                });
    }

}
