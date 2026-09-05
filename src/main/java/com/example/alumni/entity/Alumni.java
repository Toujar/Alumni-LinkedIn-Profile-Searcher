package com.example.alumni.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a persisted alumni profile.
 *
 * Deduplication strategy: if the PhantomBuster LinkedIn Search Export provides
 * a profileUrl (LinkedIn canonical URL), we store it and use it as a unique
 * natural key. On re-search, any profile whose profileUrl already exists is
 * skipped rather than inserted again. If profileUrl is absent (rare edge case),
 * the profile is persisted without deduplication because we have no stable
 * identifier to compare against — this is documented in the README.
 *
 * Column naming note: "current_role" is a reserved keyword in PostgreSQL
 * (it is a built-in function). The column is therefore named "job_title"
 * at the database level and mapped here via @Column(name = "job_title").
 */
@Entity
@Table(name = "alumni")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alumni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Maps to the "job_title" column.
     * "current_role" is a PostgreSQL reserved keyword and cannot be used
     * as an unquoted column name.
     */
    @Column(name = "job_title")
    private String currentRole;

    @Column(nullable = false)
    private String university;

    private String location;

    @Column(name = "linkedin_headline", length = 512)
    private String linkedinHeadline;

    @Column(name = "passout_year")
    private Integer passoutYear;

    /**
     * LinkedIn canonical profile URL used as the deduplication key.
     * Uniqueness is enforced by a partial index in the Flyway migration
     * (WHERE profile_url IS NOT NULL) so duplicate NULL values are allowed.
     */
    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
