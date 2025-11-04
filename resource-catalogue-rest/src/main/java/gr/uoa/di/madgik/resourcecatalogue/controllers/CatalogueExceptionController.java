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

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import gr.uoa.di.madgik.catalogue.controller.GenericExceptionController;
import gr.uoa.di.madgik.catalogue.exception.ServerError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashSet;
import java.util.Set;

@RestControllerAdvice
public class CatalogueExceptionController extends GenericExceptionController {

    /**
     * Transforms every thrown exception to a {@link ServerError} response.
     *
     * @param req http servlet request
     * @param ex  the thrown exception
     * @return {@link ServerError}
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        HttpStatusCode status = HttpStatus.BAD_REQUEST;

        InvalidFormatException exception = (InvalidFormatException) ex.getCause();
        String field = exception.getPathReference();
        field = field.substring(field.indexOf("[")).replaceAll("\"", "");
        String message = "Field %s: %s".formatted(field, exception.getOriginalMessage());
        ServerError serverError = new ServerError(status, req, ex);
        serverError.setMessage(message);
        return ResponseEntity
                .status(status)
                .body(serverError);
    }

    /**
     * Traverses the cause chain of a given {@link Throwable} to find the first occurrence
     * of a specific exception type.
     *
     * @param <T>        the type of the exception to search for
     * @param ex         the root exception to inspect; may be null
     * @param targetType the class object of the exception type to find
     * @return the first exception in the cause chain that is an instance of {@code targetType},
     * or {@code null} if none is found
     */
    private static <T extends Throwable> T findCause(Throwable ex, Class<T> targetType) {
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
