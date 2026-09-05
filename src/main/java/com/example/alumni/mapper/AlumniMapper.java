package com.example.alumni.mapper;

import com.example.alumni.dto.external.LinkedInProfileResult;
import com.example.alumni.dto.response.AlumniResponse;
import com.example.alumni.entity.Alumni;
import org.springframework.stereotype.Component;

/**
 * Converts between the external PhantomBuster profile model, the JPA entity,
 * and the outbound API response DTO.
 *
 * Keeping this logic in a dedicated class:
 *   - keeps both the service and the controller free of mapping noise
 *   - makes individual mapping steps easy to unit-test in isolation
 *   - provides a single place to adjust field mappings if PhantomBuster
 *     changes its output schema
 */
@Component
public class AlumniMapper {

    /**
     * Converts a raw PhantomBuster profile result into a persistable entity.
     *
     * Field mapping:
     *   fullName   → name
     *   job        → linkedinHeadline  (headline shown in LinkedIn search results)
     *   currentJob → currentRole       (resolved company/title detail)
     *   location   → location
     *   profileUrl → profileUrl        (deduplication key)
     *
     * The university and passoutYear fields are carried forward from the
     * original search request because PhantomBuster does not return them
     * as structured data — they are contextual to the search itself.
     *
     * @param profile     raw profile from PhantomBuster
     * @param university  university name from the original search request
     * @param passoutYear graduation year from the request (may be null)
     * @return a new, non-persisted Alumni entity
     */
    public Alumni toEntity(LinkedInProfileResult profile,
                           String university,
                           Integer passoutYear) {
        return Alumni.builder()
                .name(profile.getFullName())
                .currentRole(profile.getCurrentJob())
                .university(university)
                .location(profile.getLocation())
                .linkedinHeadline(profile.getJob())
                .passoutYear(passoutYear)
                .profileUrl(profile.getProfileUrl())
                .build();
    }

    /**
     * Converts a persisted entity into the outbound response DTO.
     * The entity's internal ID and audit fields are intentionally excluded.
     *
     * @param alumni persisted entity
     * @return response DTO safe to expose through the REST API
     */
    public AlumniResponse toResponse(Alumni alumni) {
        return AlumniResponse.builder()
                .name(alumni.getName())
                .currentRole(alumni.getCurrentRole())
                .university(alumni.getUniversity())
                .location(alumni.getLocation())
                .linkedinHeadline(alumni.getLinkedinHeadline())
                .passoutYear(alumni.getPassoutYear())
                .build();
    }
}
