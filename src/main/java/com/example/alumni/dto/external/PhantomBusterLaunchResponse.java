package com.example.alumni.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response from POST /api/v2/agents/launch.
 *
 * PhantomBuster returns a JSON object whose top-level key is "containerId" —
 * the unique ID for this specific Phantom run. We use it to poll for results.
 *
 * All unknown fields are ignored so the model stays stable if PhantomBuster
 * adds extra fields in future API versions.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhantomBusterLaunchResponse {

    private String containerId;
}
