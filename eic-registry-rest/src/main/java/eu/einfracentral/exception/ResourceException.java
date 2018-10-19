package eu.einfracentral.exception;

import org.springframework.http.HttpStatus;

public class ResourceException extends RuntimeException {
    private HttpStatus status;

    public ResourceException(HttpStatus status) {
        this("ResourceException", status);
    }

    public ResourceException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }

    public ResourceException(Exception e, HttpStatus status) {
        super(e);
        this.setStatus(status);
    }

    public HttpStatus getStatus() {
        return status;
    }

    private void setStatus(HttpStatus status) {
        this.status = status;
    }
}
