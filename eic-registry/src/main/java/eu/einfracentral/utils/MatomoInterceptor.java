package eu.einfracentral.utils;

import org.piwik.java.tracking.PiwikRequest;
import org.piwik.java.tracking.PiwikTracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;

@Component
public class MatomoInterceptor extends HandlerInterceptorAdapter {

    @Value("apitracking.matomo.site")
    private Integer siteId;

    @Value("apitracking.matomo.host")
    private String matomoUrl;

    private PiwikTracker piwikTracker = null;

    @PostConstruct
    public void init() {
        if (matomoUrl != null)
            this.piwikTracker = new PiwikTracker(matomoUrl);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);

        if (piwikTracker != null) {
            PiwikRequest piwikRequest = new PiwikRequest(siteId, new URL(request.getRequestURI()));

            piwikTracker.sendRequestAsync(piwikRequest);
        }
    }
}
