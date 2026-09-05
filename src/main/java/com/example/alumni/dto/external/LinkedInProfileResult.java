package com.example.alumni.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single profile entry from the LinkedIn Search Export result
 * array returned inside {@link PhantomBusterResultResponse#getResultObject()}.
 *
 * Field names match the exact keys PhantomBuster's LinkedIn Search Export
 * Phantom outputs (confirmed from official documentation):
 *
 *   profileUrl   – canonical LinkedIn profile URL; used as deduplication key
 *   fullName     – display name
 *   job          – headline / current job title shown in search results
 *   location     – geographic location string
 *   summary      – profile summary / about section
 *   currentJob   – current company / position detail
 *   query        – the search query that produced this result
 *
 * Unknown fields are ignored so new Phantom output fields don't break parsing.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkedInProfileResult {

    private String profileUrl;
    private String fullName;

    /** LinkedIn headline (job title line shown in search results). */
    private String job;

    private String location;
    private String summary;
    private String currentJob;

    /** The search query that produced this result — useful for debugging. */
    private String query;
}
