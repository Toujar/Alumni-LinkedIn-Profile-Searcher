package com.example.alumni.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Payload sent to POST https://api.phantombuster.com/api/v2/agents/launch.
 *
 * Fields:
 *   id       – the PhantomBuster Agent (Phantom) ID configured in properties
 *   argument – JSON string containing the Phantom's input; for LinkedIn Search
 *              Export this is {"linkedInSearchUrl": "..."}
 *
 * The {@code argument} is a JSON-encoded string per the PhantomBuster API spec
 * (the API reference documents it as a String or Object; we send it as a
 * pre-serialised JSON string for predictable behaviour).
 */
@Getter
@Builder
public class PhantomBusterLaunchRequest {

    @JsonProperty("id")
    private String agentId;

    /**
     * JSON-encoded argument string, e.g.:
     * {"linkedInSearchUrl":"https://www.linkedin.com/search/results/people/?keywords=..."}
     */
    private String argument;
}
