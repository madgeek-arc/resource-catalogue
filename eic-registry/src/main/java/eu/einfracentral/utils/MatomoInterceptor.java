package eu.einfracentral.utils;

import org.apache.log4j.Logger;
import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.matomo.java.tracking.servlet.JavaxHttpServletWrapper;
import org.matomo.java.tracking.servlet.ServletMatomoRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MatomoInterceptor implements AsyncHandlerInterceptor {

    private static Logger logger = Logger.getLogger(MatomoInterceptor.class);

    @Value("${apitracking.matomo.site:#{null}}")
    private Integer siteId;

    @Value("${apitracking.matomo.host:#{null}}")
    private String matomoUrl;

    private MatomoTracker MatomoTracker = null;

    @PostConstruct
    public void init() {
        if (matomoUrl != null && !"".equals(matomoUrl)) {
            this.MatomoTracker = new MatomoTracker(TrackerConfiguration.builder().apiEndpoint(URI.create(matomoUrl)).build());
            if (siteId == null) {
                logger.warn("'apitracking.matomo.site' is undefined. Using default value 1");
                siteId = 1;
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        if (MatomoTracker != null) {

            if (request.getHeader("Referer") == null) {
                logger.debug("Referer is null. That probably means that the call did not come from the portal. Logging!");

                MatomoRequest MatomoRequest = ServletMatomoRequest.fromServletRequest(
                        JavaxHttpServletWrapper.fromHttpServletRequest(request))
                        .siteId(siteId)
                        .actionUrl(request.getRequestURL().toString())
                        .build();

                MatomoRequest.setActionName(request.getRequestURI());
                MatomoRequest.setUserId(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
                MatomoRequest.setReferrerUrl(null);

                MatomoTracker.sendRequestAsync(MatomoRequest);
            } else {
                logger.debug("Referer is not null. Ignoring!");
            }
        }
    }
}
