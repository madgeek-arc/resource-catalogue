package eu.einfracentral.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends RuntimeException {
    private HttpStatus status;

    public ValidationException(HttpStatus status) {
        this("ValidationException", status);
    }

    public ValidationException(String msg) {
        this(msg, HttpStatus.CONFLICT);
    }

    public ValidationException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }

    public ValidationException(Exception e, HttpStatus status) {
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