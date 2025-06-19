/**
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
import gr.uoa.di.madgik.resourcecatalogue.config.AmsProperties;
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
import java.util.Map;

@Service
@Primary
public class AmsJmsService extends DefaultJmsService implements JmsService {

    private static final Logger logger = LoggerFactory.getLogger(AmsJmsService.class);

    private final RestTemplate restTemplate;
    private final AmsProperties amsProperties;

    @Value("${catalogue.jms.prefix}")
    private String jmsPrefix;

    public AmsJmsService(JmsTemplate jmsTopicTemplate,
                         JmsTemplate jmsQueueTemplate,
                         RestTemplate restTemplate,
                         AmsProperties amsProperties) {
        super(jmsTopicTemplate, jmsQueueTemplate);
        this.restTemplate = restTemplate;
        this.amsProperties = amsProperties;
    }

    @Override
    public void convertAndSendTopic(String messageDestination, Object message) {
        try {
            publishTopic(messageDestination.replace(".", "-"), message);
            super.convertAndSendTopic(jmsPrefix + "." + messageDestination, message);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                createTopic(messageDestination.replace(".", "-"));
            }
            throw new RuntimeException("Error sending topic", e);
        }
    }

    @Override
    public void convertAndSendQueue(String messageDestination, Object message) {
        super.convertAndSendTopic(jmsPrefix + "." + messageDestination, message);
    }

    //region Topics
    public void createTopic(String topic) {
        if (!amsProperties.isEnabled()) {
            logger.warn("AMS is disabled, skipping execution.");
        }
        HttpEntity<String> request = createHttpRequest();
        restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics/" + topic,
                HttpMethod.PUT, request, String.class);
    }

    public String deleteTopic(String topic) {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics/" + topic,
                HttpMethod.DELETE, request, String.class);
        return response.getBody();
    }

    public String getTopic(String topic) {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics/" + topic,
                HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getAllTopics() {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response = restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics",
                HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public void publishTopic(String topic, Object message) {
        if (!amsProperties.isEnabled()) {
            logger.warn("AMS is disabled, skipping execution.");
        }
        HttpEntity<String> request = createHttpRequest(message);
        restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics/" + topic + ":publish",
                HttpMethod.POST, request, String.class);
        logger.info("Sending JMS to topic: {} via AMS", topic);
    }
    //endregion

    //region Subscriptions
    public String getTopicSubscriptions(String topic) {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response =
                restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics/" + topic + "/subscriptions",
                        HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getAllSubscriptions() {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
        HttpEntity<String> request = createHttpRequest();
        ResponseEntity<String> response =
                restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/subscriptions",
                        HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public void createSubscriptionForTopic(String topic, String name) {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
        String bodyUrl = amsProperties.getHost() + "/" + amsProperties.getProject() + "/topics/" + topic;
        HttpEntity<String> request = createHttpRequest(bodyUrl);
        try {
            restTemplate.exchange(amsProperties.getHost() + "/" + amsProperties.getProject() + "/subscriptions/" + name,
                    HttpMethod.PUT, request, String.class);
        } catch (HttpClientErrorException e) {
            logger.info(e.getMessage());
        }
    }
    //endregion

    private HttpEntity<String> createHttpRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsProperties.getKey());

        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> createHttpRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsProperties.getKey());

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

    private HttpEntity<String> createHttpRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsProperties.getKey());

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
}
