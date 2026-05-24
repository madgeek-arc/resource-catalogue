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
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Primary
public class DelegatingJmsService implements JmsService {

    private static final Logger logger = LoggerFactory.getLogger(DelegatingJmsService.class);

    private final List<JmsPublisher> publishers;

    public DelegatingJmsService(List<JmsPublisher> publishers) {
        this.publishers = new ArrayList<>(publishers);
        AnnotationAwareOrderComparator.sort(this.publishers);
    }

    @Override
    public void convertAndSendTopic(String messageDestination, Object message) {
        publishers.forEach(publisher -> sendTopic(publisher, messageDestination, message));
    }

    @Override
    public void convertAndSendQueue(String messageDestination, Object message) {
        publishers.forEach(publisher -> sendQueue(publisher, messageDestination, message));
    }

    private void sendTopic(JmsPublisher publisher, String messageDestination, Object message) {
        try {
            publisher.convertAndSendTopic(messageDestination, message);
        } catch (RuntimeException e) {
            logger.warn("JMS publisher '{}' failed to send topic '{}'. Continuing with remaining publishers.",
                    publisher.getClass().getSimpleName(), messageDestination, e);
        }
    }

    private void sendQueue(JmsPublisher publisher, String messageDestination, Object message) {
        try {
            publisher.convertAndSendQueue(messageDestination, message);
        } catch (RuntimeException e) {
            logger.warn("JMS publisher '{}' failed to send queue '{}'. Continuing with remaining publishers.",
                    publisher.getClass().getSimpleName(), messageDestination, e);
        }
    }
}
