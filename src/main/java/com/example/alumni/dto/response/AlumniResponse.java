package com.example.alumni.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Outbound representation of a single alumni profile.
 * Never exposes the internal entity ID or database internals.
 */
@Getter
@Builder
public class AlumniResponse {

    private String name;
    private String currentRole;
    private String university;
    private String location;
    private String linkedinHeadline;
    private Integer passoutYear;
}
