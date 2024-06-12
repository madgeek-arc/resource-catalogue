package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.dto.ServerError;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Order(1)
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);


    /**
     * Transforms every thrown exception to a {@link ServerError} response.
     *
     * @param req http servlet request
     * @param ex  the thrown exception
     * @return {@link ServerError}
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        HttpStatus status = getStatusFromException(ex);

        if (ex instanceof ResourceException) {
            logger.info(ex.getMessage(), ex);
            status = ((ResourceException) ex).getStatus();
        } else if (ex instanceof HttpClientErrorException) {
            logger.info(ex.getMessage(), ex);
            status = ((HttpClientErrorException) ex).getStatusCode();
        } else if (ex instanceof AccessDeniedException) {
            logger.info(ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof InsufficientAuthenticationException) {
            logger.info(ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ResourceAlreadyExistsException) {
            logger.info(ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof ResourceNotFoundException || ex instanceof gr.uoa.di.madgik.registry.exception.ResourceNotFoundException) {
            logger.info(ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            status = HttpStatus.NOT_FOUND;
        } else {
            logger.error(ex.getMessage(), ex);
        }
        return ResponseEntity
                .status(status)
                .body(new ServerError(status, req, ex));
    }

    /**
     * Get http status code from {@link ResponseStatus} annotation using reflection, if it exists, else return {@literal HttpStatus.INTERNAL_SERVER_ERROR}.
     *
     * @param exception thrown exception
     * @return {@link HttpStatus}
     */
    private HttpStatus getStatusFromException(Exception exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ResponseStatus annotation = exception.getClass().getAnnotation(ResponseStatus.class);
        if (annotation != null) {
            status = annotation.value();
        }
        return status;
    }

    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());

        //Get all errors
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(x -> x.getDefaultMessage())
                .collect(Collectors.toList());

        body.put("errors", errors);

        return new ResponseEntity<>(body, headers, status);

    }
}
