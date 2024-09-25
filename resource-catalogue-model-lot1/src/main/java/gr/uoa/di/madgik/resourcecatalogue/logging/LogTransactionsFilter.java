package gr.uoa.di.madgik.resourcecatalogue.logging;

import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
@Order(0)
public class LogTransactionsFilter extends AbstractLogContextFilter {

    private static final String TRANSACTION_ID = "transaction_id";
    private static final String USER_INFO = "user_info";
    private static final String REMOTE_IP = "remote_ip";

    @Override
    public void editMDC(MDCAdapter mdc, ServletRequest request, ServletResponse response) {
        String transactionId = UUID.randomUUID().toString();
        mdc.put(TRANSACTION_ID, transactionId);
        appendUserInfo(mdc);
        if (StringUtils.hasText(((HttpServletRequest) request).getHeader("X-Forwarded-For"))) {
            mdc.put(REMOTE_IP, ((HttpServletRequest) request).getHeader("X-Forwarded-For"));
        } else {
            mdc.put(REMOTE_IP, request.getRemoteAddr());
        }
    }

    public static String getTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }

    private void appendUserInfo(MDCAdapter mdc) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            try {
                User user = User.of(authentication);
                if (user != null) {
                    mdc.put(USER_INFO, user.toString());
                }
            } catch (InsufficientAuthenticationException e) {
                mdc.put(USER_INFO, authentication.toString());
            }
        }
    }
}
