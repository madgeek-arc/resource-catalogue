package gr.uoa.di.madgik.resourcecatalogue.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public abstract class AbstractLogContextFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLogContextFilter.class);

    public void destroy() {
    }

    /**
     * Uses MDCAdapter to edit MDC. Passes down ServletRequest and ServletResponse for more customized functionality.
     *
     * @param mdc      {@link MDCAdapter}
     * @param request  {@link ServletRequest}
     * @param response {@link ServletResponse}
     */
    public abstract void editMDC(MDCAdapter mdc, ServletRequest request, ServletResponse response);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {

        logger.trace("Editing MDC");
        editMDC(MDC.getMDCAdapter(), request, response);

        try {
            chain.doFilter(request, response);
        } finally {
            logger.trace("Clearing MDC");
            MDC.clear();
        }
    }

}
