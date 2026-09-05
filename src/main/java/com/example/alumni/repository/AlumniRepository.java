package com.example.alumni.repository;

import com.example.alumni.entity.Alumni;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Alumni} entities.
 *
 * The custom finder supports deduplication: before persisting a profile we
 * check whether a record with the same LinkedIn profile URL already exists.
 * Only profiles with a non-null profileUrl are deduplicated this way.
 */
public interface AlumniRepository extends JpaRepository<Alumni, Long> {

    /**
     * Looks up an existing alumni record by LinkedIn profile URL.
     * Used to avoid duplicate inserts when the same profile appears
     * across multiple search runs.
     *
     * @param profileUrl the canonical LinkedIn profile URL
     * @return an Optional containing the existing entity, or empty if not found
     */
    Optional<Alumni> findByProfileUrl(String profileUrl);
}
