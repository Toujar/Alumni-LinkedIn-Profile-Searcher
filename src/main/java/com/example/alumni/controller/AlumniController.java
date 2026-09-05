package com.example.alumni.controller;

import com.example.alumni.dto.request.AlumniSearchRequest;
import com.example.alumni.dto.response.AlumniResponse;
import com.example.alumni.dto.response.ApiResponse;
import com.example.alumni.service.AlumniService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for alumni profile operations.
 *
 * Responsibilities:
 *   - Parse and validate incoming HTTP requests.
 *   - Delegate all business logic to {@link AlumniService}.
 *   - Return correctly structured, appropriately status-coded responses.
 *
 * This controller contains no business logic, no database access, and no
 * direct PhantomBuster interaction — all of that lives in the service layer.
 */
@RestController
@RequestMapping("/api/alumni")
public class AlumniController {

    private static final Logger log = LoggerFactory.getLogger(AlumniController.class);

    private final AlumniService alumniService;

    public AlumniController(AlumniService alumniService) {
        this.alumniService = alumniService;
    }

    /**
     * POST /api/alumni/search
     *
     * Searches LinkedIn for alumni profiles matching the given university and
     * designation, persists new profiles, and returns the matched set.
     *
     * Request body:
     * <pre>
     * {
     *   "university":  "University of XYZ",   // required
     *   "designation": "Software Engineer",    // required
     *   "passoutYear": 2020                    // optional
     * }
     * </pre>
     *
     * Successful response (HTTP 200):
     * <pre>
     * {
     *   "status": "success",
     *   "data":   [ { ... alumni profile ... } ]
     * }
     * </pre>
     *
     * @param request validated search parameters
     * @return 200 OK with matched profiles; 400 on validation failure;
     *         502 on PhantomBuster error; 500 on unexpected error
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<AlumniResponse>> searchAlumni(
            @Valid @RequestBody AlumniSearchRequest request) {

        log.info("POST /api/alumni/search — university='{}', designation='{}', passoutYear={}",
                request.getUniversity(), request.getDesignation(), request.getPassoutYear());

        List<AlumniResponse> results = alumniService.searchAndSaveAlumni(request);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/alumni/all
     *
     * Returns all alumni profiles currently saved in the database.
     *
     * Successful response (HTTP 200):
     * <pre>
     * {
     *   "status": "success",
     *   "data":   [ { ... alumni profile ... } ]
     * }
     * </pre>
     *
     * @return 200 OK with all saved profiles (empty list if none exist)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<AlumniResponse>> getAllAlumni() {
        log.info("GET /api/alumni/all");
        List<AlumniResponse> results = alumniService.getAllAlumni();
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
