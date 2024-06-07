package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DefaultJmsService implements JmsService {

    private static final Logger logger = LogManager.getLogger(DefaultJmsService.class);
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
