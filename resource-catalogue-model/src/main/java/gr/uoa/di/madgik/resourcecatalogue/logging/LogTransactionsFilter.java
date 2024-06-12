package gr.uoa.di.madgik.resourcecatalogue.logging;

import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
@Order(0)
public class LogTransactionsFilter extends AbstractLogContextFilter {

    private static final String TRANSACTION_ID = "transaction_id";
    private static final String REMOTE_IP = "remote_ip";

    @Override
    public void editMDC(MDCAdapter mdc, ServletRequest request, ServletResponse response) {
        String transactionId = UUID.randomUUID().toString();
        mdc.put(TRANSACTION_ID, transactionId);
        if (StringUtils.hasText(((HttpServletRequest) request).getHeader("X-Forwarded-For"))) {
            mdc.put(REMOTE_IP, ((HttpServletRequest) request).getHeader("X-Forwarded-For"));
        } else {
            mdc.put(REMOTE_IP, request.getRemoteAddr());
        }
    }

    public static String getTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }
}
