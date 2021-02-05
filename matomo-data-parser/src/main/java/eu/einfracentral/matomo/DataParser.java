package eu.einfracentral.matomo;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import eu.einfracentral.registry.service.EventService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;


@Configuration
@EnableScheduling
@Component
public class DataParser {

    private static final Logger logger = LogManager.getLogger(DataParser.class);
    private static final String serviceVisitsTemplate = "%s/index.php?token_auth=%s&module=API&method=Events.getName&format=JSON&idSite=%s&period=day&date=today";
    private String serviceVisits;
    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private EventService eventService;

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
        String matomoHost = "https://providers.eosc-portal.eu/matomo";
        String matomoToken = "c30102091f049bc3dceff09abafeb76c";
        String matomoSiteId = "1";
        serviceVisits = String.format(serviceVisitsTemplate, matomoHost, matomoToken, matomoSiteId, "%s");
    }

//    @Scheduled(fixedDelay = (5 * 60 * 1000))
    @Scheduled(fixedDelay = (60 * 1000))
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
                for (Map.Entry<String, Float> entry : results.entrySet()) {
                    System.out.println(entry.getKey() + ":" + entry.getValue().toString());
                }
            } catch (Exception e) {
                logger.error("Cannot retrieve visits for all Services\nMatomo response: {}\n", json, e);
            }
        }
        try {
            postEventsToDatabase(results);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        } catch (NumberParseException e) {
            e.printStackTrace();
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
            try{
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    logger.error("Could not retrieve analytics from matomo\nResponse Code: {}\nResponse Body: {}",
                            responseEntity.getStatusCode().toString(), responseEntity.getBody());
                }
                return responseEntity.getBody();
            } catch (IllegalArgumentException e) {
                logger.info ("URI is not absolute");
            }
        } catch (RuntimeException e) {
            logger.error("Could not retrieve analytics from matomo", e);
        }
        return "";
    }

    public void postEventsToDatabase(Map<String, Float> events) throws ResourceNotFoundException, NumberParseException {
//        for (Map.Entry<String, Float> entry : events.entrySet()){
//            logger.info("Posting Internal Event for Service {} with value {}", entry.getKey(), entry.getValue());
//            if (eventService != null){
//                eventService.setInternal(entry.getKey(), entry.getValue());
//            } else {
//                logger.info("Empty Internal View");
//            }
//        }
        logger.info("Check Check Check");
    }
}

