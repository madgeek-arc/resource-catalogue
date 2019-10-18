package eu.einfracentral.registry.controller;

import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.openminted.registry.core.service.ServiceException;
import javassist.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.UUID;

@ControllerAdvice
@Order(1)
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LogManager.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = {IllegalStateException.class, IllegalArgumentException.class})
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        logger.error("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value = {ResourceNotFoundException. class, NotFoundException.class,
            ChangeSetPersister.NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        logger.warn("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = {AuthenticationException.class, UnauthorizedClientException.class,
            UnauthorizedUserException.class})
    protected ResponseEntity<Object> handleUnauthorized(RuntimeException ex, WebRequest request) {
        logger.warn("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = {AccessDeniedException.class})
    protected ResponseEntity<Object> handleForbidden(RuntimeException ex, WebRequest request) {
        logger.error("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ResponseBody
    @ExceptionHandler(value = {ResourceException.class})
    protected ResponseEntity<Object> handleResourceException(ResourceException ex, WebRequest request) {
        logger.error("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), ex.getStatus(), request);
    }

    @ExceptionHandler(value = {ValidationException.class})
    protected ResponseEntity<Object> handleValidationException(ValidationException ex, WebRequest request) {
        logger.error("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), ex.getStatus(), request);
    }

    @ExceptionHandler(value = {ServiceException.class})
    protected ResponseEntity<Object> handleServiceException(RuntimeException ex, WebRequest request) {
        logger.error("", ex);
        return handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {RuntimeException.class})
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        String response = String.format("Please send the following error code to the system administrators. Error Code: '%s'", errorId);
        logger.error("Error Code: " + errorId, errorId, ex);
        return handleExceptionInternal(ex, response,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
