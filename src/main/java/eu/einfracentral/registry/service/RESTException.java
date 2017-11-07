package eu.einfracentral.registry.service;
import org.springframework.http.HttpStatus;
/**
 * Created by pgl on 10/10/17.
 */
public class RESTException extends RuntimeException {
    private HttpStatus status;
    public RESTException(HttpStatus status) {
        this("RESTException", status);
    }
    public RESTException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }
    public RESTException(Exception e, HttpStatus status) {
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
