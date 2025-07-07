/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.matomo.java.tracking.servlet.JakartaHttpServletWrapper;
import org.matomo.java.tracking.servlet.ServletMatomoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.net.URI;

@Component
public class MatomoInterceptor implements AsyncHandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MatomoInterceptor.class);

    @Value("${apitracking.matomo.site:#{null}}")
    private Integer siteId;

    @Value("${apitracking.matomo.host:#{null}}")
    private String matomoUrl;

    private MatomoTracker matomoTracker = null;

    @PostConstruct
    public void init() {
        if (matomoUrl != null && !matomoUrl.isEmpty()) {
            this.matomoTracker = new MatomoTracker(TrackerConfiguration.builder().apiEndpoint(URI.create(matomoUrl)).build());
            if (siteId == null) {
                logger.warn("'apitracking.matomo.site' is undefined. Using default value 1");
                siteId = 1;
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        if (matomoTracker != null) {

            if (request.getHeader("Referer") == null) {
                logger.debug("Referer is null. That probably means that the call did not come from the portal. Logging!");

                MatomoRequest matomoRequest = ServletMatomoRequest.fromServletRequest(
                                JakartaHttpServletWrapper.fromHttpServletRequest(request))
                        .siteId(siteId)
                        .actionUrl(request.getRequestURL().toString())
                        .build();

                matomoRequest.setActionName(request.getRequestURI());
                matomoRequest.setUserId(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
                matomoRequest.setReferrerUrl(null);

                matomoTracker.sendRequestAsync(matomoRequest);
            } else {
                logger.debug("Referer is not null. Ignoring!");
            }
        }
    }
}
