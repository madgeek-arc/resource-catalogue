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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class AmsJmsService extends DefaultJmsService implements JmsService {

    private static final Logger logger = LoggerFactory.getLogger(AmsJmsService.class);

    private final RestTemplate restTemplate;

    @Value("${ams.jms.host}")
    private String host;

    @Value("${ams.jms.key}")
    private String key;

    @Value("${ams.jms.projects}")
    private List<String> projects; //TODO: revisit getFirst() if more projects are added

    public AmsJmsService(JmsTemplate jmsTopicTemplate, JmsTemplate jmsQueueTemplate, RestTemplate restTemplate) {
        super(jmsTopicTemplate, jmsQueueTemplate);
        this.restTemplate = restTemplate;
    }

    //region Topics
    public void createTopic(String topic) {
        HttpEntity<String> request = createHttpRequest();
        restTemplate.exchange(host + "/" + projects.getFirst() + "/topics/" + topic,
                HttpMethod.PUT, request, String.class);
    }

    public String deleteTopic(String topic) {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(host + "/" + projects.getFirst() + "/topics/" + topic,
                HttpMethod.DELETE, request, String.class);
        return response.getBody();
    }

    public String getTopic(String topic) {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(host + "/" + projects.getFirst() + "/topics/" + topic,
                HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getAllTopics() {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(host + "/" + projects.getFirst() + "/topics",
                HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public void publishTopic(String topic, Object message) {
        HttpEntity<String> request = createHttpRequest(message);
        restTemplate.exchange(host + "/" + projects.getFirst() + "/topics/" + topic + ":publish",
                HttpMethod.POST, request, String.class);
        logger.info("Sending JMS to topic: {} via AMS", topic);
    }
    //endregion

    //region Subscriptions
    public String getTopicSubscriptions(String topic) {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response =
                restTemplate.exchange(host + "/" + projects.getFirst() + "/topics/" + topic + "/subscriptions",
                        HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getAllSubscriptions() {
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response =
                restTemplate.exchange(host + "/" + projects.getFirst() + "/subscriptions",
                        HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public void createSubscriptionForTopic(String topic, String name) {
        String bodyUrl = host + "/" + projects.getFirst() + "/topics/" + topic;
        HttpEntity<String> request = createHttpRequest(bodyUrl);
        try {
            restTemplate.exchange(host + "/" + projects.getFirst() + "/subscriptions/" + name,
                    HttpMethod.PUT, request, String.class);
        } catch (HttpClientErrorException e) {
            logger.info(e.getMessage());
        }
    }
    //endregion

    public HttpEntity<String> createHttpRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", key);

        return new HttpEntity<>(headers);
    }

    public HttpEntity<String> createHttpRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", key);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload;
        try {
            String jsonMessage = objectMapper.writeValueAsString(body);
            String base64EncodedData = Base64.getEncoder().encodeToString(jsonMessage.getBytes());
            Map<String, Object> pubSubMessage = createMessageForTopic(base64EncodedData);
            jsonPayload = objectMapper.writeValueAsString(pubSubMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
        return new HttpEntity<>(jsonPayload, headers);
    }

    public HttpEntity<String> createHttpRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", key);

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

    private Map<String, Object> createMessageForSubscription(String url) {
        return Map.of(
                "topic", url,
                "ackDeadlineSeconds", 10
        );
    }

    @Override
    public void convertAndSendTopic(String messageDestination, Object message) {
        try {
            publishTopic(messageDestination.replace(".", "-"), message);
            super.convertAndSendTopic(messageDestination, message);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                createTopic(messageDestination.replace(".", "-"));
            }
            throw new RuntimeException("Error sending topic", e);
        }
    }

    @Override
    public void convertAndSendQueue(String messageDestination, Object message) {
        super.convertAndSendTopic(messageDestination, message);
    }
}
