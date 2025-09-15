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
import gr.uoa.di.madgik.resourcecatalogue.config.AmsProperties;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class AmsJmsService extends DefaultJmsService implements JmsService {

    private static final Logger logger = LoggerFactory.getLogger(AmsJmsService.class);

    private final WebClient webClient;
    private final AmsProperties amsProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${catalogue.jms.prefix}")
    private String jmsPrefix;

    public AmsJmsService(JmsTemplate jmsTopicTemplate,
                         JmsTemplate jmsQueueTemplate,
                         WebClient.Builder webClientBuilder,
                         AmsProperties amsProperties) {
        super(jmsTopicTemplate, jmsQueueTemplate);
        this.webClient = webClientBuilder.build();
        this.amsProperties = amsProperties;
    }

    @Override
    public void convertAndSendTopic(String messageDestination, Object message) {
        try {
            publishTopic(messageDestination.replace(".", "-"), message);
            super.convertAndSendTopic(jmsPrefix + "." + messageDestination, message);
        } catch (WebClientResponseException e) {
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
            return;
        }
        sendRequest(buildUrl("/topics/" + topic), HttpMethod.PUT, createHttpRequest(), false);
    }

    public String deleteTopic(String topic) {
        ensureEnabled();
        return sendRequest(buildUrl("/topics/" + topic), HttpMethod.DELETE, createHttpRequest(), true);
    }

    public String getTopic(String topic) {
        ensureEnabled();
        return sendRequest(buildUrl("/topics/" + topic), HttpMethod.GET, createHttpRequest(), true);
    }

    public String getAllTopics() {
        ensureEnabled();
        return sendRequest(buildUrl("/topics"), HttpMethod.GET, createHttpRequest(), true);
    }

    public void publishTopic(String topic, Object message) {
        if (!amsProperties.isEnabled()) {
            logger.warn("AMS is disabled, skipping execution.");
            return;
        }
        HttpEntity<String> request = createHttpRequestForTopic(message);
        sendRequest(buildUrl("/topics/" + topic + ":publish"), HttpMethod.POST, request, false);
        logger.info("Sending JMS to topic: {} via AMS", topic);
    }
    //endregion

    //region Subscriptions
    public String getTopicSubscriptions(String topic) {
        ensureEnabled();
        return sendRequest(buildUrl("/topics/" + topic + "/subscriptions"), HttpMethod.GET, createHttpRequest(), true);
    }

    public String getAllSubscriptions() {
        ensureEnabled();
        return sendRequest(buildUrl("/subscriptions"), HttpMethod.GET, createHttpRequest(), true);
    }

    public void createSubscriptionForTopic(String topic, String name) {
        ensureEnabled();
        String topicUrl = buildUrl("/topics/" + topic);
        HttpEntity<String> request = createHttpRequestForSubscription(topicUrl);
        try {
            sendRequest(buildUrl("/subscriptions/" + name), HttpMethod.PUT, request, false);
        } catch (WebClientResponseException e) {
            logger.info(e.getMessage());
        }
    }
    //endregion

    //region Helpers

    private String buildUrl(String path) {
        return amsProperties.getHost() + "/" + amsProperties.getProject() + path;
    }

    private void ensureEnabled() {
        if (!amsProperties.isEnabled()) {
            throw new IllegalStateException("AMS Service is disabled.");
        }
    }

    private String sendRequest(String url, HttpMethod method, HttpEntity<String> request, boolean expectBody) {
        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(url)
                .headers(headers -> headers.addAll(request.getHeaders()));

        WebClient.ResponseSpec responseSpec = request.getBody() != null
                ? spec.bodyValue(request.getBody()).retrieve()
                : spec.retrieve();

        return expectBody
                ? responseSpec.bodyToMono(String.class).block()
                : responseSpec.toBodilessEntity().block().toString();
    }

    private HttpEntity<String> createHttpRequest() {
        HttpHeaders headers = createHeaders();
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> createHttpRequestForTopic(Object body) {
        HttpHeaders headers = createHeaders();
        try {
            String jsonMessage = objectMapper.writeValueAsString(body);
            String base64EncodedData = Base64.getEncoder().encodeToString(jsonMessage.getBytes());
            Map<String, Object> pubSubMessage = createMessageForTopic(base64EncodedData);
            String jsonPayload = objectMapper.writeValueAsString(pubSubMessage);
            return new HttpEntity<>(jsonPayload, headers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
    }

    private HttpEntity<String> createHttpRequestForSubscription(String topicUrl) {
        HttpHeaders headers = createHeaders();
        try {
            Map<String, Object> pubSubMessage = createMessageForSubscription(topicUrl);
            String jsonPayload = objectMapper.writeValueAsString(pubSubMessage);
            return new HttpEntity<>(jsonPayload, headers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", amsProperties.getKey());
        return headers;
    }

    private Map<String, Object> createMessageForTopic(String base64EncodedData) {
        return Map.of(
                "messages", List.of(
                        Map.of(
                                "attributes", Map.of("source", "Service Catalogue"),
                                "data", base64EncodedData
                        )
                )
        );
    }

    private Map<String, Object> createMessageForSubscription(String topicUrl) {
        return Map.of(
                "topic", topicUrl,
                "ackDeadlineSeconds", 10
        );
    }
    //endregion
}
