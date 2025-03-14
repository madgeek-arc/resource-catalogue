package gr.uoa.di.madgik.resourcecatalogue.manager.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service
public class AmsClient {

    private static final Logger logger = LoggerFactory.getLogger(AmsClient.class);

    private final RestTemplate restTemplate;

    @Value("${ams.jms.host}")
    private String amsHost;

    @Value("${ams.jms.key}")
    private String amsKey;

    public AmsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public HttpEntity<String> createHttpRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsKey);

        return new HttpEntity<>(headers);
    }

    public HttpEntity<String> createHttpRequest(Object message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsKey);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload;
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            String base64EncodedData = Base64.getEncoder().encodeToString(jsonMessage.getBytes());
            Map<String, Object> pubSubMessage = createMessageForTopic(base64EncodedData);
            jsonPayload = objectMapper.writeValueAsString(pubSubMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
        return new HttpEntity<>(jsonPayload, headers);
    }

    private Map<String, Object> createMessageForTopic(String base64EncodedData) {
        return Map.of(
                "messages", Collections.singletonList(
                        Map.of(
                                "attributes", Map.of("source", "Service Catalogue"),
                                "data", base64EncodedData
                        )
                )
        );
    }

    public HttpEntity<String> createHttpRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsKey);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload;
        try {
            Map<String, Object> pubSubMessage = createMessageForSubscription(url);
            jsonPayload = objectMapper.writeValueAsString(pubSubMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
        return new HttpEntity<>(jsonPayload, headers);
    }

    private Map<String, Object> createMessageForSubscription(String url) {
        return Map.of(
                "topic", url,
                "ackDeadlineSeconds", 10
        );
    }

    //region Topics
    public String createTopic(String topic) {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsHost + "/topics/" + topic, HttpMethod.PUT, request, String.class);
        return response.getBody();
    }

    public String getTopics() {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsHost + "/topics", HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getTopic(String topic) {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsHost + "/topics/" + topic, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public void publishTopic(String topic, Object message) {
        HttpEntity<String> request = createHttpRequest(message);
        try {
            restTemplate.exchange(amsHost + "/topics/" + topic + ":publish", HttpMethod.POST, request, String.class);
        } catch (HttpClientErrorException e) {
            logger.info(e.getMessage());
        }
    }
    //endregion

    //region Subscriptions
    public String getSubscriptionsPerTopic(String topic) {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsHost + "/topics/" + topic + "/subscriptions",
                HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getSubscriptions() {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsHost + "/subscriptions", HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public void createSubscription(String topic, String name) {
        String bodyUrl = amsHost + "/topics/" + topic;
        HttpEntity<String> request = createHttpRequest(bodyUrl);
        try {
            restTemplate.exchange(amsHost + "/subscriptions/" + name, HttpMethod.PUT, request, String.class);
        } catch (HttpClientErrorException e) {
            logger.info(e.getMessage());
        }
    }
    //endregion
}
