package com.example.alumni.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

/**
 * Uniform envelope for all successful API responses.
 *
 * <pre>
 * {
 *   "status": "success",
 *   "data": [...]
 * }
 * </pre>
 *
 * The {@code data} field is always a list so callers have a consistent shape
 * regardless of whether the result is a single item or a collection.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String status;
    private final List<T> data;

    private ApiResponse(String status, List<T> data) {
        this.status = status;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(List<T> data) {
        return new ApiResponse<>("success", data);
    }
}
