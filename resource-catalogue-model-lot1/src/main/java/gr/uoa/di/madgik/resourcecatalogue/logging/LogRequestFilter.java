package gr.uoa.di.madgik.resourcecatalogue.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import gr.uoa.di.madgik.resourcecatalogue.utils.RequestUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class LogRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LogRequestFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.info(RequestUtils.getUrlWithParams(request));
        filterChain.doFilter(request, response);
    }
}
