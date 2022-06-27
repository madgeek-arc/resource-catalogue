package eu.einfracentral.registry.controller;

import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.openminted.registry.core.exception.ServerError;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
@Order(1)
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LogManager.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = {IllegalStateException.class, IllegalArgumentException.class})
    protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
        logger.warn("", ex);
        logger.debug(ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value = {ResourceNotFoundException.class, NotFoundException.class,
            ChangeSetPersister.NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(Exception ex, WebRequest request) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = {AuthenticationException.class, UnauthorizedClientException.class,
            UnauthorizedUserException.class})
    protected ResponseEntity<Object> handleUnauthorized(Exception ex, WebRequest request) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = {AccessDeniedException.class})
    protected ResponseEntity<Object> handleForbidden(Exception ex, WebRequest request) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = {ResourceException.class})
    protected ResponseEntity<Object> handleResourceException(ResourceException ex, WebRequest request) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), ex.getStatus(), request);
    }

    @ExceptionHandler(value = {ValidationException.class})
    protected ResponseEntity<Object> handleValidationException(ValidationException ex, WebRequest request) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), ex.getStatus(), request);
    }

    @ExceptionHandler(value = {ServiceException.class})
    protected ResponseEntity<Object> handleServiceException(Exception ex, WebRequest request) {
        logger.error("", ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, ex);
        return handleExceptionInternal(ex, se, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {RuntimeException.class})
    protected ResponseEntity<Object> handleRuntimeException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        String response = String.format("Please send the following error code to the system administrators. Error Code: %s", errorId);
        logger.error("Error Code: {}", errorId, ex);
        String url = ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        ServerError se = new ServerError(url, new RuntimeException(response));
        return handleExceptionInternal(ex, se, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
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
