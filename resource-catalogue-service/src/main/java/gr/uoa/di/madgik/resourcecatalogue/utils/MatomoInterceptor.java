package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.matomo.java.tracking.servlet.JavaxHttpServletWrapper;
import org.matomo.java.tracking.servlet.ServletMatomoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

@Component
public class MatomoInterceptor implements AsyncHandlerInterceptor {

    private static Logger logger = LoggerFactory.getLogger(MatomoInterceptor.class);

    @Value("${apitracking.matomo.site:#{null}}")
    private Integer siteId;

    @Value("${apitracking.matomo.host:#{null}}")
    private String matomoUrl;

    private MatomoTracker matomoTracker = null;

    @PostConstruct
    public void init() {
        if (matomoUrl != null && !"".equals(matomoUrl)) {
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
                                JavaxHttpServletWrapper.fromHttpServletRequest((javax.servlet.http.HttpServletRequest) request))
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
