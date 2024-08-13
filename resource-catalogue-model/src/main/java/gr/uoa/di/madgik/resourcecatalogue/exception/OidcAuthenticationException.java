package gr.uoa.di.madgik.resourcecatalogue.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Authentication does not contain OidcUser.")
// 401
public class OidcAuthenticationException extends RuntimeException {

    public OidcAuthenticationException() {
        super();
    }

    public OidcAuthenticationException(String msg) {
        super(msg);
    }
}
