/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.catalogue.controller.GenericExceptionController;
import gr.uoa.di.madgik.catalogue.exception.ServerError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

@ControllerAdvice
public class CatalogueExceptionController extends GenericExceptionController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueExceptionController.class);

    /**
     * Transforms every thrown exception to a {@link ServerError} response.
     *
     * @param req http servlet request
     * @param ex  the thrown exception
     * @return {@link ServerError}
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        HttpStatusCode status = getStatusFromException(ex);

        //TODO: populate according to Service Catalogue needs
        MalformedURLException malformedUrlEx = findCause(ex, MalformedURLException.class);
        if (malformedUrlEx != null) {
            logger.info(malformedUrlEx.getMessage());
            logger.debug(malformedUrlEx.getMessage(), ex);
            status = HttpStatus.BAD_REQUEST;
        } else {
            return super.handleException(req, ex);
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

    /**
     * Traverses the cause chain of a given {@link Throwable} to find the first occurrence
     * of a specific exception type.
     *
     * @param <T> the type of the exception to search for
     * @param ex the root exception to inspect; may be null
     * @param targetType the class object of the exception type to find
     * @return the first exception in the cause chain that is an instance of {@code targetType},
     *         or {@code null} if none is found
     */
    public static <T extends Throwable> T findCause(Throwable ex, Class<T> targetType) {
        Set<Throwable> visited = new HashSet<>();
        while (ex != null && visited.add(ex)) {
            if (targetType.isInstance(ex)) {
                return targetType.cast(ex);
            }
            ex = ex.getCause();
        }
        return null;
    }
}
