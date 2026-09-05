package com.example.alumni.service;

import com.example.alumni.client.PhantomBusterClient;
import com.example.alumni.dto.external.LinkedInProfileResult;
import com.example.alumni.dto.request.AlumniSearchRequest;
import com.example.alumni.dto.response.AlumniResponse;
import com.example.alumni.entity.Alumni;
import com.example.alumni.mapper.AlumniMapper;
import com.example.alumni.repository.AlumniRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the alumni search workflow:
 *
 * <pre>
 * Controller
 *     ↓
 * AlumniServiceImpl
 *     ↓
 * PhantomBusterClient          – external HTTP (isolated behind interface)
 *     ↓
 * post-filter by passout year  – LinkedIn has no native year filter
 *     ↓
 * deduplicate via profileUrl   – skip profiles already in the DB
 *     ↓
 * AlumniPersistenceService.saveAll()  – single transactional batch insert
 *     ↓
 * AlumniMapper.toResponse()    – map entities → response DTOs
 *     ↓
 * Controller
 * </pre>
 *
 * <h2>Pass-out year handling</h2>
 * PhantomBuster's LinkedIn Search Export does not expose a graduation-year
 * filter natively. When {@code passoutYear} is supplied it is stored on every
 * persisted entity for data-provenance purposes. Filtering is documented as
 * a known limitation in the README.
 *
 * <h2>Deduplication</h2>
 * Profiles carrying a {@code profileUrl} are checked against the database
 * before insertion. Existing records are skipped. Profiles without a
 * profileUrl are always inserted — no stable key is available to compare.
 *
 * <h2>Transaction boundary</h2>
 * The PhantomBuster HTTP call happens entirely outside any transaction. The
 * transactional boundary is owned by {@link AlumniPersistenceService#saveAll},
 * which is a separate Spring bean. This ensures Spring's AOP proxy intercepts
 * the call correctly and that no database connection is held open during the
 * network round-trip.
 */
@Service
public class AlumniServiceImpl implements AlumniService {

    private static final Logger log = LoggerFactory.getLogger(AlumniServiceImpl.class);

    private final PhantomBusterClient    phantomBusterClient;
    private final AlumniRepository       alumniRepository;
    private final AlumniMapper           alumniMapper;
    private final AlumniPersistenceService persistenceService;

    public AlumniServiceImpl(PhantomBusterClient phantomBusterClient,
                             AlumniRepository alumniRepository,
                             AlumniMapper alumniMapper,
                             AlumniPersistenceService persistenceService) {
        this.phantomBusterClient = phantomBusterClient;
        this.alumniRepository    = alumniRepository;
        this.alumniMapper        = alumniMapper;
        this.persistenceService  = persistenceService;
    }

    // -------------------------------------------------------------------------
    // Search and persist
    // -------------------------------------------------------------------------

    @Override
    public List<AlumniResponse> searchAndSaveAlumni(AlumniSearchRequest request) {
        log.info("Starting alumni search — university='{}', designation='{}', passoutYear={}",
                request.getUniversity(), request.getDesignation(), request.getPassoutYear());

        // 1. Call PhantomBuster — all HTTP concerns isolated in the client
        List<LinkedInProfileResult> externalProfiles =
                phantomBusterClient.searchAlumni(request);

        log.info("PhantomBuster returned {} profile(s) before filtering.", externalProfiles.size());

        // 2. Optional passout-year note (LinkedIn has no native year filter)
        logPassoutYearNote(externalProfiles, request.getPassoutYear());

        if (externalProfiles.isEmpty()) {
            log.info("No profiles returned. Returning empty list.");
            return List.of();
        }

        // 3. Build the list of new profiles (deduplicate against DB)
        List<Alumni> toSave = buildNewProfiles(
                externalProfiles, request.getUniversity(), request.getPassoutYear());

        if (toSave.isEmpty()) {
            log.info("All profiles were duplicates; nothing new to persist.");
            return List.of();
        }

        // 4. Persist — transactional batch via separate bean (avoids proxy bypass)
        List<Alumni> saved = persistenceService.saveAll(toSave);

        // 5. Map persisted entities to response DTOs
        List<AlumniResponse> responses = saved.stream()
                .map(alumniMapper::toResponse)
                .toList();

        log.info("Alumni search complete — {} new profile(s) persisted and returned.",
                responses.size());
        return responses;
    }

    // -------------------------------------------------------------------------
    // Retrieve all
    // -------------------------------------------------------------------------

    @Override
    public List<AlumniResponse> getAllAlumni() {
        log.info("Fetching all saved alumni profiles.");
        List<AlumniResponse> responses = alumniRepository.findAll()
                .stream()
                .map(alumniMapper::toResponse)
                .toList();
        log.info("Returning {} saved alumni profile(s).", responses.size());
        return responses;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the list of Alumni entities that are genuinely new (not yet in
     * the database). Profiles with a non-blank profileUrl are checked against
     * existing records; profiles without a URL are always treated as new.
     */
    private List<Alumni> buildNewProfiles(List<LinkedInProfileResult> profiles,
                                          String university,
                                          Integer passoutYear) {
        List<Alumni> toSave = new ArrayList<>();
        int skippedCount = 0;

        for (LinkedInProfileResult profile : profiles) {
            if (isDuplicate(profile)) {
                skippedCount++;
                log.debug("Skipping duplicate profile — profileUrl={}", profile.getProfileUrl());
                continue;
            }
            toSave.add(alumniMapper.toEntity(profile, university, passoutYear));
        }

        if (skippedCount > 0) {
            log.info("Deduplication: skipped {} duplicate profile(s).", skippedCount);
        }
        return toSave;
    }

    /**
     * Returns {@code true} if this profile already exists in the database.
     * Only possible when the profile carries a non-blank LinkedIn URL.
     */
    private boolean isDuplicate(LinkedInProfileResult profile) {
        if (!StringUtils.hasText(profile.getProfileUrl())) {
            return false;
        }
        return alumniRepository.findByProfileUrl(profile.getProfileUrl()).isPresent();
    }

    /**
     * Logs a note about passout-year handling when a year was requested.
     * LinkedIn Search Export does not return a graduation year, so no
     * client-side filtering is possible — the year is stored for provenance.
     */
    private void logPassoutYearNote(List<LinkedInProfileResult> profiles, Integer passoutYear) {
        if (passoutYear != null) {
            log.info("passoutYear={} requested. LinkedIn Search Export does not return a " +
                     "graduation year field; all {} profile(s) are retained. " +
                     "The year is stored on each saved entity.",
                     passoutYear, profiles.size());
        }
    }
}
