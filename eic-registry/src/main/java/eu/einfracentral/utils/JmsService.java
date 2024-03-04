package eu.einfracentral.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jms.JmsSecurityException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JmsService {

    private static final Logger logger = LogManager.getLogger(JmsService.class);
    private final JmsTemplate jmsTopicTemplate;
    private final JmsTemplate jmsQueueTemplate;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 60000;
    private String messageDestination;
    private Object message;

    public JmsService(JmsTemplate jmsTopicTemplate, JmsTemplate jmsQueueTemplate) {
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.jmsQueueTemplate = jmsQueueTemplate;
    }

    public void convertAndSendTopic(String messageDestination, Object message) {
        try {
            logger.info("Sending JMS to topic: {}", messageDestination);
            jmsTopicTemplate.convertAndSend(messageDestination, message);
        } catch (JmsSecurityException e) {
            logger.info("JMS failed. Error: {}", e.getMessage(), e);
            setMessageInfo(messageDestination, message);
            retrySending();
        }
    }

    public void convertAndSendQueue(String messageDestination, Object message) {
        try {
            logger.info("Sending JMS to queue: {}", messageDestination);
            jmsQueueTemplate.convertAndSend(messageDestination, message);
        } catch (JmsSecurityException e) {
            logger.info("JMS failed. Error: {}", e.getMessage(), e);
            setMessageInfo(messageDestination, message);
            retrySending();
        }
    }

    public void setMessageInfo(String messageDestination, Object message) {
        this.messageDestination = messageDestination;
        this.message = message;
    }

    @Async
    @Scheduled(fixedDelay = RETRY_DELAY_MS)
    public void retrySending() {
        if (messageDestination == null || message == null) {
            return;
        }

        boolean sentSuccessfully = false;
        int attempts = 0;

        while (!sentSuccessfully && attempts < MAX_RETRY_ATTEMPTS) {
            try {
                logger.info("Retrying sending JMS message to topic: {}", messageDestination);
                jmsTopicTemplate.convertAndSend(messageDestination, message);
                sentSuccessfully = true;
                logger.info("JMS message sent successfully on retry.");
            } catch (JmsSecurityException e) {
                attempts++;
                logger.error("JMS sending failed on retry. Attempt {} out of {}. Error: {}", attempts, MAX_RETRY_ATTEMPTS, e.getMessage(), e);
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    logger.error("Maximum retry attempts reached. Cannot send JMS message to topic: {}", messageDestination);
                } else {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
