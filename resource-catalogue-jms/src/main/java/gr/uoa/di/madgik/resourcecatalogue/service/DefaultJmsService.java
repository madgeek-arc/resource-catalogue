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

import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class DefaultJmsService implements JmsService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJmsService.class);
    private final JmsTemplate jmsTopicTemplate;
    private final JmsTemplate jmsQueueTemplate;

    public DefaultJmsService(JmsTemplate jmsTopicTemplate, JmsTemplate jmsQueueTemplate) {
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.jmsQueueTemplate = jmsQueueTemplate;
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(value = 6000))
    public void convertAndSendTopic(String messageDestination, Object message) {
        logger.info("Sending JMS to topic: {}", messageDestination);
        jmsTopicTemplate.convertAndSend(messageDestination, message);
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(value = 6000))
    public void convertAndSendQueue(String messageDestination, Object message) {
        logger.info("Sending JMS to topic: {}", messageDestination);
        jmsQueueTemplate.convertAndSend(messageDestination, message);
    }
}
