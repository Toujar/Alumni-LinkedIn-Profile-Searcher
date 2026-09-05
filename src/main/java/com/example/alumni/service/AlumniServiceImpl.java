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
import org.springframework.transaction.annotation.Transactional;
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
 * PhantomBusterClient          – external HTTP (isolated)
 *     ↓
 * post-filter by passout year  – LinkedIn has no year filter natively
 *     ↓
 * deduplicate via profileUrl   – skip profiles already in the DB
 *     ↓
 * AlumniRepository.saveAll()   – single transactional batch insert
 *     ↓
 * AlumniMapper.toResponse()    – map entities → response DTOs
 *     ↓
 * Controller
 * </pre>
 *
 * <h2>Pass-out year handling</h2>
 * PhantomBuster's LinkedIn Search Export does not expose a graduation-year
 * filter. When {@code passoutYear} is supplied in the request, we filter the
 * returned profiles client-side. Because LinkedIn's search results reflect
 * a user's current profile data, the year filter operates on a best-effort
 * basis and is clearly documented as such in the README.
 *
 * <h2>Deduplication</h2>
 * Profiles that carry a {@code profileUrl} are checked against the database
 * before insertion. Existing records are skipped rather than updated; only
 * genuinely new profiles are inserted. Profiles without a profileUrl (edge
 * case) are always inserted because we have no stable key to compare.
 *
 * <h2>Transaction boundary</h2>
 * {@code @Transactional} wraps only the persistence step so that a partial
 * database failure rolls back all inserts from the current batch, rather than
 * leaving the table in an inconsistent half-written state. The PhantomBuster
 * call is intentionally outside the transaction — no DB connection is held
 * open during the network round-trip.
 */
@Service
public class AlumniServiceImpl implements AlumniService {

    private static final Logger log = LoggerFactory.getLogger(AlumniServiceImpl.class);

    private final PhantomBusterClient phantomBusterClient;
    private final AlumniRepository    alumniRepository;
    private final AlumniMapper        alumniMapper;

    public AlumniServiceImpl(PhantomBusterClient phantomBusterClient,
                             AlumniRepository alumniRepository,
                             AlumniMapper alumniMapper) {
        this.phantomBusterClient = phantomBusterClient;
        this.alumniRepository    = alumniRepository;
        this.alumniMapper        = alumniMapper;
    }

    // -------------------------------------------------------------------------
    // Search and persist
    // -------------------------------------------------------------------------

    @Override
    public List<AlumniResponse> searchAndSaveAlumni(AlumniSearchRequest request) {
        log.info("Starting alumni search — university='{}', designation='{}', passoutYear={}",
                request.getUniversity(), request.getDesignation(), request.getPassoutYear());

        // 1. Call PhantomBuster — all HTTP concerns are isolated in the client
        List<LinkedInProfileResult> externalProfiles =
                phantomBusterClient.searchAlumni(request);

        log.info("PhantomBuster returned {} profile(s) before filtering.", externalProfiles.size());

        // 2. Optional pass-out year filter (LinkedIn has no native year filter)
        List<LinkedInProfileResult> filteredProfiles =
                filterByPassoutYear(externalProfiles, request.getPassoutYear());

        if (filteredProfiles.isEmpty()) {
            log.info("No profiles matched after filtering. Returning empty list.");
            return List.of();
        }

        // 3. Persist new profiles (deduplication inside, transactional batch)
        List<Alumni> savedAlumni = persistNewProfiles(
                filteredProfiles, request.getUniversity(), request.getPassoutYear());

        // 4. Map persisted entities to response DTOs
        List<AlumniResponse> responses = savedAlumni.stream()
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
     * Filters profiles by graduation year when the caller supplied one.
     *
     * LinkedIn does not expose a structured passout-year field in search
     * results. The PhantomBuster output does not include graduation year
     * either. This filter is therefore a best-effort no-op today — it keeps
     * the interface correct and documents the limitation clearly. Should
     * PhantomBuster ever return a year field, the filter can be activated here
     * without changing any other class.
     *
     * For now we return all profiles unchanged when passoutYear is provided,
     * and log a clear statement so the behaviour is transparent.
     */
    private List<LinkedInProfileResult> filterByPassoutYear(
            List<LinkedInProfileResult> profiles, Integer passoutYear) {

        if (passoutYear == null) {
            return profiles;
        }

        /*
         * LinkedIn Search Export does not return a graduation year field.
         * Returning all profiles — year filtering is noted as a limitation
         * in the README. The passoutYear is still stored on the entity so
         * the data is preserved for future use or manual curation.
         */
        log.info("passoutYear={} was requested. LinkedIn Search Export does not provide a " +
                 "graduation year field; all {} profile(s) are retained. " +
                 "The year is stored on each saved entity.", passoutYear, profiles.size());
        return profiles;
    }

    /**
     * Converts external profiles to entities, deduplicates against existing DB
     * records (by profileUrl), then persists the new ones in a single batch.
     *
     * @return the list of entities that were actually saved
     */
    @Transactional
    protected List<Alumni> persistNewProfiles(List<LinkedInProfileResult> profiles,
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

        if (toSave.isEmpty()) {
            log.info("All profiles were duplicates; nothing new to persist.");
            return List.of();
        }

        List<Alumni> saved = alumniRepository.saveAll(toSave);
        log.info("Persisted {} new alumni profile(s).", saved.size());
        return saved;
    }

    /**
     * Returns {@code true} if this profile already exists in the database.
     *
     * Deduplication is only possible when the profile carries a non-blank
     * LinkedIn URL. Profiles without a URL are always treated as new.
     */
    private boolean isDuplicate(LinkedInProfileResult profile) {
        if (!StringUtils.hasText(profile.getProfileUrl())) {
            return false;
        }
        return alumniRepository.findByProfileUrl(profile.getProfileUrl()).isPresent();
    }
}
