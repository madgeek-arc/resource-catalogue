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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consecutive-failure circuit breaker guarding calls to a service. Not a Spring
 * bean - each caller owns its own instance, scoped to its own service route and its own
 * threshold/reset-ms configuration.
 */
final class CircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

    private final int failureThreshold;
    private final long resetMs;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openUntilMillis = new AtomicLong(0);

    CircuitBreaker(int failureThreshold, long resetMs) {
        this.failureThreshold = failureThreshold;
        this.resetMs = resetMs;
    }

    boolean isOpen() {
        return System.currentTimeMillis() < openUntilMillis.get();
    }

    void onSuccess() {
        consecutiveFailures.set(0);
    }

    void onFailure(String opLabel, Exception e) {
        logger.warn("Federation call {} failed: {}", opLabel, e.getMessage());
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            openUntilMillis.set(System.currentTimeMillis() + resetMs);
            logger.warn("Federation circuit breaker for {} opened after {} consecutive failures; "
                    + "skipping calls for {} ms", opLabel, failures, resetMs);
        }
    }
}
