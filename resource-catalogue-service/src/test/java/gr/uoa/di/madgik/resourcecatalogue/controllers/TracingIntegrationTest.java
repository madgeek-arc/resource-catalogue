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

package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.resourcecatalogue.integration.BaseIntegrationTest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.filter.ServerHttpObservationFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the wiring that puts trace ids into logs and error responses: a real (non-noop) tracer and the HTTP
 * observation filter that opens a span per request.
 */
class TracingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private Tracer tracer;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CatalogueExceptionController exceptionController;

    @Test
    void usesOpenTelemetryTracer() {
        assertThat(tracer).isInstanceOf(OtelTracer.class);
    }

    @Test
    void registersHttpObservationFilter() {
        ResolvableType type = ResolvableType.forClassWithGenerics(
                FilterRegistrationBean.class, ServerHttpObservationFilter.class);

        assertThat(context.getBeanNamesForType(type)).isNotEmpty();
    }

    @Test
    void errorResponseCarriesCurrentTraceId() {
        Span span = tracer.nextSpan().name("test").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("unreadable", new MockHttpInputMessage(new byte[0]));

            ProblemDetail body = exceptionController
                    .handleException(new MockHttpServletRequest("POST", "/test"), ex)
                    .getBody();

            assertThat(body).isNotNull();
            assertThat(body.getProperties()).containsEntry("traceId", span.context().traceId());
        } finally {
            span.end();
        }
    }
}
