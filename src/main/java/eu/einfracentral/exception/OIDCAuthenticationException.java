package eu.einfracentral.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.UNAUTHORIZED, reason="Authentication not an instance of OIDCAuthentication")  // 401
public class OIDCAuthenticationException extends RuntimeException{

    public OIDCAuthenticationException() {
        super();
    }

    public OIDCAuthenticationException(String msg) {
        super(msg);
    }
}
