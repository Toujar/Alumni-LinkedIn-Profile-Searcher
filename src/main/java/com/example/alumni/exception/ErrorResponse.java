package com.example.alumni.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Uniform error envelope returned for all non-2xx responses.
 *
 * <pre>
 * {
 *   "status":    "error",
 *   "message":   "Meaningful description",
 *   "timestamp": "2026-09-04T15:30:00"
 * }
 * </pre>
 *
 * Stack traces and internal exception details are never exposed.
 */
@Getter
public class ErrorResponse {

    private final String status = "error";
    private final String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
