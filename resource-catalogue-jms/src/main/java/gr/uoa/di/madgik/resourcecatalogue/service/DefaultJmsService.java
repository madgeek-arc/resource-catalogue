/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.resourcecatalogue.utils.JmsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Order(1)
@ConditionalOnProperty(name = "registry.jms.enabled", havingValue = "true")
public class DefaultJmsService implements JmsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJmsService.class);
    private final JmsTemplate jmsTopicTemplate;
    private final JmsTemplate jmsQueueTemplate;
    private final String jmsPrefix;

    public DefaultJmsService(@Autowired(required = false) JmsTemplate jmsTopicTemplate,
                             @Autowired(required = false) JmsTemplate jmsQueueTemplate,
                             @Value("${catalogue.jms.prefix}") String jmsPrefix) {
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.jmsQueueTemplate = jmsQueueTemplate;
        this.jmsPrefix = jmsPrefix;
    }

    @Override
    public void convertAndSendTopic(String messageDestination, Object message) {
        if (jmsTopicTemplate != null) {
            String destination = buildDestination(messageDestination);
            logger.info("Sending JMS to topic: {}", destination);
            jmsTopicTemplate.convertAndSend(destination, message);
        }
    }

    @Override
    public void convertAndSendQueue(String messageDestination, Object message) {
        if (jmsQueueTemplate != null) {
            String destination = buildDestination(messageDestination);
            logger.info("Sending JMS to queue: {}", destination);
            jmsQueueTemplate.convertAndSend(destination, message);
        }
    }

    private String buildDestination(String messageDestination) {
        return jmsPrefix + "." + messageDestination;
    }
}
