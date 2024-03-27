package gr.uoa.di.madgik.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class JmsService {

    private static final Logger logger = LogManager.getLogger(JmsService.class);
    private final JmsTemplate jmsTopicTemplate;
    private final JmsTemplate jmsQueueTemplate;

    public JmsService(JmsTemplate jmsTopicTemplate, JmsTemplate jmsQueueTemplate) {
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
