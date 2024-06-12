package gr.uoa.di.madgik.resourcecatalogue.logging;

import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import org.slf4j.spi.MDCAdapter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Component
public class LogUser extends AbstractLogContextFilter {

    private static final String USER_INFO = "user_info";

    @Override
    public void editMDC(MDCAdapter mdc, ServletRequest request, ServletResponse response) {
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
