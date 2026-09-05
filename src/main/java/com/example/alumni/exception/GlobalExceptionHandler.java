package com.example.alumni.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 *
 * All exceptions are logged at an appropriate level before the sanitised
 * error response is returned. Stack traces and internal details are never
 * included in the response body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Jakarta Bean Validation failures (missing / invalid request fields).
     * Collects all field errors into a single readable message.
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    /**
     * PhantomBuster API communication failures.
     * HTTP 502 Bad Gateway — the error is in the upstream dependency.
     */
    @ExceptionHandler(PhantomBusterException.class)
    public ResponseEntity<ErrorResponse> handlePhantomBusterException(
            PhantomBusterException ex) {

        log.error("PhantomBuster API failure: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("External API error: " + ex.getMessage()));
    }

    /**
     * Domain-level alumni data problems.
     * HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(AlumniDataException.class)
    public ResponseEntity<ErrorResponse> handleAlumniDataException(
            AlumniDataException ex) {

        log.error("Alumni data error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Data processing error: " + ex.getMessage()));
    }

    /**
     * Spring Data / database failures.
     * HTTP 500 Internal Server Error.
     * The cause is logged but the generic message is returned to the caller.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex) {

        log.error("Database error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("A database error occurred. Please try again later."));
    }

    /**
     * Wrong HTTP method for a known endpoint (e.g. GET on a POST-only route).
     * HTTP 405 Method Not Allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Method not allowed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Catch-all for any unexpected exception not handled above.
     * HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred. Please try again later."));
    }
}
