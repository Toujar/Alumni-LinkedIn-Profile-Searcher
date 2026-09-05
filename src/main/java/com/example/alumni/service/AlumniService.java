package com.example.alumni.service;

import com.example.alumni.dto.request.AlumniSearchRequest;
import com.example.alumni.dto.response.AlumniResponse;

import java.util.List;

/**
 * Application-layer contract for alumni profile operations.
 *
 * The controller depends only on this interface, keeping it decoupled from
 * the concrete implementation and straightforward to test with Mockito.
 */
public interface AlumniService {

    /**
     * Searches LinkedIn for alumni profiles matching the given criteria,
     * persists any new profiles, and returns the matched set.
     *
     * @param request validated search parameters
     * @return list of matched alumni profiles; never null, may be empty
     */
    List<AlumniResponse> searchAndSaveAlumni(AlumniSearchRequest request);

    /**
     * Retrieves all alumni profiles currently persisted in the database.
     *
     * @return list of all saved profiles; never null, may be empty
     */
    List<AlumniResponse> getAllAlumni();
}
