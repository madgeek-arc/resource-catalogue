package eu.einfracentral.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jms.JmsSecurityException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class JmsService {

    private static final Logger logger = LogManager.getLogger(JmsService.class);
    private final JmsTemplate jmsTopicTemplate;
    private final JmsTemplate jmsQueueTemplate;

    private JmsService(JmsTemplate jmsTopicTemplate, JmsTemplate jmsQueueTemplate) {
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.jmsQueueTemplate = jmsQueueTemplate;
    }

    public void convertAndSendTopic(String messageDestination, Object message) {
        try {
            logger.info("Sending JMS to topic: {}", messageDestination);
            jmsTopicTemplate.convertAndSend(messageDestination, message);
        } catch (JmsSecurityException e) {
            logger.info("JMS failed. Error: {}", e.getMessage(), e);
        }
    }

    public void convertAndSendQueue(String messageDestination, Object message) {
        try {
            logger.info("Sending JMS to queue: {}", messageDestination);
            jmsQueueTemplate.convertAndSend(messageDestination, message);
        } catch (JmsSecurityException e) {
            logger.info("JMS failed. Error: {}", e.getMessage(), e);
        }
    }
}
