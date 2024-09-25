package gr.uoa.di.madgik.resourcecatalogue.logging;

import gr.uoa.di.madgik.resourcecatalogue.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
