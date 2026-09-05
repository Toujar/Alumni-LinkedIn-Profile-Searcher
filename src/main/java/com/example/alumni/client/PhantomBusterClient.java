package com.example.alumni.client;

import com.example.alumni.dto.external.LinkedInProfileResult;
import com.example.alumni.dto.request.AlumniSearchRequest;
import com.example.alumni.exception.PhantomBusterException;

import java.util.List;

/**
 * Abstraction over PhantomBuster HTTP communication.
 *
 * The service layer depends only on this interface, keeping it completely
 * decoupled from HTTP, polling logic, and PhantomBuster-specific DTOs.
 * This also makes the service trivially testable by substituting a mock.
 *
 * Contract:
 *   - Accepts a search request describing what to look for.
 *   - Returns a list of matched LinkedIn profiles (may be empty).
 *   - Throws {@link PhantomBusterException} for any external API failure,
 *     timeout, or unexpected response — callers should not need to handle
 *     lower-level HTTP exceptions.
 */
public interface PhantomBusterClient {

    /**
     * Searches LinkedIn for alumni profiles matching the given criteria.
     *
     * Internally this launches a PhantomBuster LinkedIn Search Export Phantom,
     * polls until the run completes, then parses and returns the result set.
     *
     * @param request the search parameters (university, designation, optional year)
     * @return list of matched profiles; never null, may be empty
     * @throws PhantomBusterException if the external API call fails for any reason
     */
    List<LinkedInProfileResult> searchAlumni(AlumniSearchRequest request);
}
