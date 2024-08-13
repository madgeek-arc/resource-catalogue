package gr.uoa.di.madgik.resourcecatalogue.matomo;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.EventService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;


@Component
public class DataParser {

    private static final Logger logger = LogManager.getLogger(DataParser.class);
    private static final String SERVICE_VISITS_TEMPLATE = "%s/index.php?token_auth=%s&module=API&method=Events.getNameFromActionId&idSubtable=1&format=JSON&idSite=%s&period=day&date=yesterday";
    private static final String SERVICE_RATINGS_TEMPLATE = "%s/index.php?token_auth=%s&module=API&method=Events.getNameFromActionId&idSubtable=3&format=JSON&idSite=%s&period=day&date=yesterday";
    private static final String SERVICE_ADD_TO_PROJECT_TEMPLATE = "%s/index.php?token_auth=%s&module=API&method=Events.getNameFromActionId&idSubtable=2&format=JSON&idSite=%s&period=day&date=yesterday";
    private String serviceVisits;
    private String serviceRatings;
    private String serviceAddToProject;
    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private final EventService eventService;

    @Value("${matomoHost:localhost}")
    private String matomoHost;

    @Value("${matomoToken:}")
    private String matomoToken;

    @Value("${matomoSiteId:1}")
    private String matomoSiteId;

    @Autowired
    DataParser(EventService eventService) {
        this.eventService = eventService;
    }

    @PostConstruct
    void postConstruct() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        String authorizationHeader = "";
        headers.add("Authorization", authorizationHeader);
        serviceVisits = String.format(SERVICE_VISITS_TEMPLATE, matomoHost, matomoToken, matomoSiteId);
        serviceAddToProject = String.format(SERVICE_ADD_TO_PROJECT_TEMPLATE, matomoHost, matomoToken, matomoSiteId);
        serviceRatings = String.format(SERVICE_RATINGS_TEMPLATE, matomoHost, matomoToken, matomoSiteId);
    }

    @Scheduled(cron = "0 10 0 * * *") // 00.10 every day
//    @Scheduled(initialDelay = 0, fixedRate = 120000) //run every 2 min
    public void getServiceVisits() {
        JsonNode json = parse(getMatomoResponse(serviceVisits));
        Map<String, Float> results = new HashMap<>();
        if (json != null) {
            try {
                Spliterators.spliteratorUnknownSize(json.iterator(), Spliterator.NONNULL);
                for (JsonNode node : json) {
                    String serviceId = node.path("label").textValue();
                    String visits = node.path("nb_uniq_visitors").toString();
                    results.putIfAbsent(serviceId, Float.parseFloat(visits));
                }
                logger.info("Resource ID : Visits");
                for (Map.Entry<String, Float> entry : results.entrySet()) {
                    logger.info(entry.getKey() + " : " + entry.getValue().toString());
                }
            } catch (Exception e) {
                logger.error("Cannot retrieve ratings for all Services\nMatomo response: {}\n", json, e);
            }
        }
        int eventType = 1; //visits
        try {
            postEventsToDatabase(results, eventType);
        } catch (ResourceNotFoundException e) {
            logger.error(e);
        } catch (NumberParseException e) {
            logger.error(e);
        }
    }

    @Scheduled(cron = "0 15 0 * * *")
    public void getServiceRatings() {
        JsonNode json = parse(getMatomoResponse(serviceRatings));
        Map<String, Float> results = new HashMap<>();
        if (json != null) {
            try {
                Spliterators.spliteratorUnknownSize(json.iterator(), Spliterator.NONNULL);
                for (JsonNode node : json) {
                    String serviceId = node.path("label").textValue();
                    String rating = node.path("avg_event_value").toString();
                    results.putIfAbsent(serviceId, Float.parseFloat(rating));
                }
                logger.info("Resource ID : Ratings");
                for (Map.Entry<String, Float> entry : results.entrySet()) {
                    logger.info(entry.getKey() + " : " + entry.getValue().toString());
                }
            } catch (Exception e) {
                logger.error("Cannot retrieve ratings for all Services\nMatomo response: {}\n", json, e);
            }
        }
        int eventType = 3; // ratings
        try {
            postEventsToDatabase(results, eventType);
        } catch (ResourceNotFoundException e) {
            logger.error(e);
        } catch (NumberParseException e) {
            logger.error(e);
        }
    }

    @Scheduled(cron = "0 20 0 * * *")
    public void getServiceAddToProject() {
        JsonNode json = parse(getMatomoResponse(serviceAddToProject));
        Map<String, Float> results = new HashMap<>();
        if (json != null) {
            try {
                Spliterators.spliteratorUnknownSize(json.iterator(), Spliterator.NONNULL);
                for (JsonNode node : json) {
                    String serviceId = node.path("label").textValue();
                    String addToProject = "1";
                    results.putIfAbsent(serviceId, Float.parseFloat(addToProject));
                }
                logger.info("Resource ID : Add to Project");
                for (Map.Entry<String, Float> entry : results.entrySet()) {
                    logger.info(entry.getKey() + " : " + entry.getValue().toString());
                }
            } catch (Exception e) {
                logger.error("Cannot retrieve ratings for all Services\nMatomo response: {}\n", json, e);
            }
        }
        int eventType = 2; // addToProject
        try {
            postEventsToDatabase(results, eventType);
        } catch (ResourceNotFoundException e) {
            logger.error(e);
        } catch (NumberParseException e) {
            logger.error(e);
        }
    }

    public static JsonNode parse(String json) {
        try {
            return new ObjectMapper(new JsonFactory()).readTree(json);
        } catch (IOException e) {
            logger.error("ERROR", e);
        }
        return null;
    }

    public String getMatomoResponse(String url) {
        try {
            HttpEntity<String> request = new HttpEntity<>(headers);
            try {
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    logger.error("Could not retrieve analytics from matomo\nResponse Code: {}\nResponse Body: {}",
                            responseEntity.getStatusCode().toString(), responseEntity.getBody());
                }
                return responseEntity.getBody();
            } catch (IllegalArgumentException e) {
                logger.info("URI is not absolute");
            }
        } catch (RuntimeException e) {
            logger.error("Could not retrieve analytics from matomo", e);
        }
        return "";
    }

    public void postEventsToDatabase(Map<String, Float> events, int eventType) throws ResourceNotFoundException, NumberParseException {
        if (eventType == 1) {
            for (Map.Entry<String, Float> entry : events.entrySet()) {
                logger.info("Posting Visit Event for Service {} with value {}", entry.getKey(), entry.getValue());
                if (eventService != null) {
                    eventService.setVisit(entry.getKey(), entry.getValue());
                } else {
                    logger.info("Empty Visit View");
                }
            }
        } else if (eventType == 2) {
            for (Map.Entry<String, Float> entry : events.entrySet()) {
                logger.info("Posting AddToProject Event for Service {} with value {}", entry.getKey(), entry.getValue());
                if (eventService != null) {
                    eventService.setAddToProject(entry.getKey(), entry.getValue());
                } else {
                    logger.info("Empty AddToProject View");
                }
            }
        } else if (eventType == 3) {
            for (Map.Entry<String, Float> entry : events.entrySet()) {
                logger.info("Posting Rating Event for Service {} with value {}", entry.getKey(), entry.getValue());
                if (eventService != null) {
                    eventService.setScheduledRating(entry.getKey(), entry.getValue());
                } else {
                    logger.info("Empty Rating View");
                }
            }
        } else {
            logger.info("No eventType specified or unknown eventType");
        }
    }
}