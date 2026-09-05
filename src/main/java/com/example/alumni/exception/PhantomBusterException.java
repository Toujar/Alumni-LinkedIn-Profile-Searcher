package com.example.alumni.exception;

/**
 * Thrown when communication with the PhantomBuster API fails.
 *
 * This covers:
 *   - HTTP 4xx / 5xx responses from PhantomBuster
 *   - Network / connection failures
 *   - Timeout waiting for a Phantom run to complete
 *   - Unexpected / unparseable response bodies
 *
 * The controller advice maps this to HTTP 502 Bad Gateway so the caller knows
 * the error originated in an upstream dependency, not in our application.
 */
public class PhantomBusterException extends RuntimeException {

    public PhantomBusterException(String message) {
        super(message);
    }

    public PhantomBusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
