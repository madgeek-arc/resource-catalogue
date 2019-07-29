package eu.einfracentral.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends RuntimeException {
    private HttpStatus status = HttpStatus.NOT_FOUND;

    public ResourceNotFoundException(HttpStatus status) {
        this("ResourceNotFoundException", status);
    }

    public ResourceNotFoundException(String msg) {
        super(msg);
    }

    public ResourceNotFoundException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }

    public ResourceNotFoundException(Exception e, HttpStatus status) {
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
