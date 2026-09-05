package com.example.alumni.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Inbound request payload for POST /api/alumni/search.
 *
 * university    – required; name of the educational institution
 * designation   – required; current job title / role to search for
 * passoutYear   – optional; graduation year used to post-filter results
 *                 (PhantomBuster does not expose a graduation-year filter
 *                  natively, so filtering happens in the service layer)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniSearchRequest {

    @NotBlank(message = "University name is required")
    private String university;

    @NotBlank(message = "Designation is required")
    private String designation;

    /**
     * Graduation year. Constrained to a realistic range:
     * 1950 covers the oldest plausible alumni; the current year + 1
     * accommodates upcoming graduates. We use 2100 as a static upper
     * ceiling — generous enough to never need changing.
     */
    @Min(value = 1950, message = "Pass-out year must be 1950 or later")
    @Max(value = 2100, message = "Pass-out year must be 2100 or earlier")
    private Integer passoutYear;
}
