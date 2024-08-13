package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface JmsService {

    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(value = 6000))
    void convertAndSendTopic(String messageDestination, Object message);

    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(value = 6000))
    void convertAndSendQueue(String messageDestination, Object message);
}
