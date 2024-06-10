package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoopJmsService implements JmsService {

    private static final Logger logger = LoggerFactory.getLogger(gr.uoa.di.madgik.resourcecatalogue.utils.JmsService.class);

    public NoopJmsService() {
    }

    public void convertAndSendTopic(String messageDestination, Object message) {
        logger.debug("No-op");
    }

    public void convertAndSendQueue(String messageDestination, Object message) {
        logger.debug("No-op");
    }

}
