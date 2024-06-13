package gr.uoa.di.madgik.resourcecatalogue.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException() {
        super("Resource with the same ID already exists.");
    }

    public ResourceAlreadyExistsException(String msg) {
        super(msg);
    }
}
