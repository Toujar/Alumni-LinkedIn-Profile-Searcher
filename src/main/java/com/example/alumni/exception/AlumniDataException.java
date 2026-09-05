package com.example.alumni.exception;

/**
 * Thrown when alumni data cannot be persisted or retrieved due to a
 * domain-level data problem (distinct from a raw database/infrastructure error
 * which surfaces as a Spring DataAccessException).
 *
 * Example usages:
 *   - The external API returns a profile with no usable fields
 *   - A data-integrity rule is violated at the service layer
 *
 * Maps to HTTP 500 in the global exception handler.
 */
public class AlumniDataException extends RuntimeException {

    public AlumniDataException(String message) {
        super(message);
    }

    public AlumniDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
