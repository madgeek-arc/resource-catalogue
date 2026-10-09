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

package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationDuplicateCheckProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link FederationDuplicateCheckProperties} actually fails application startup
 * when a property is missing or blank, rather than just relying on the annotations being present.
 * Uses {@link ApplicationContextRunner} - a real (but minimal, TestContainers-free) Spring
 * context - so it exercises the same binding + JSR-303 validation path the full app goes through.
 */
class FederationDuplicateCheckPropertiesUnitTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FederationDuplicateCheckProperties.class)
    static class TestConfig {
    }

    private static final String[] ALL_VALID_PROPERTIES = {
            "federation.duplicate-check.enabled=false",
            "federation.duplicate-check.search-url=https://federatedsearch.service.eosc-beyond.eu/api/federation",
            "federation.duplicate-check.timeout-ms=2000",
            "federation.duplicate-check.circuit-breaker-failure-threshold=5",
            "federation.duplicate-check.circuit-breaker-reset-ms=60000"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void allPropertiesPresentAndValid_startsSuccessfully() {
        contextRunner.withPropertyValues(ALL_VALID_PROPERTIES).run(context -> {
            assertThat(context).hasNotFailed();
            FederationDuplicateCheckProperties props = context.getBean(FederationDuplicateCheckProperties.class);
            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getTimeoutMs()).isEqualTo(2000L);
        });
    }

    @Test
    void missingProperty_failsStartupInsteadOfDefaulting() {
        contextRunner.withPropertyValues(
                // "enabled" intentionally omitted entirely
                "federation.duplicate-check.search-url=https://example.org",
                "federation.duplicate-check.timeout-ms=2000",
                "federation.duplicate-check.circuit-breaker-failure-threshold=5",
                "federation.duplicate-check.circuit-breaker-reset-ms=60000"
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankPropertyValue_failsStartupInsteadOfSilentlyBindingNull() {
        contextRunner.withPropertyValues(
                "federation.duplicate-check.enabled=", // present but blank, e.g. an empty override
                "federation.duplicate-check.search-url=https://example.org",
                "federation.duplicate-check.timeout-ms=2000",
                "federation.duplicate-check.circuit-breaker-failure-threshold=5",
                "federation.duplicate-check.circuit-breaker-reset-ms=60000"
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonPositiveTimeout_failsStartup() {
        contextRunner.withPropertyValues(
                "federation.duplicate-check.enabled=true",
                "federation.duplicate-check.search-url=https://example.org",
                "federation.duplicate-check.timeout-ms=0",
                "federation.duplicate-check.circuit-breaker-failure-threshold=5",
                "federation.duplicate-check.circuit-breaker-reset-ms=60000"
        ).run(context -> assertThat(context).hasFailed());
    }
}
