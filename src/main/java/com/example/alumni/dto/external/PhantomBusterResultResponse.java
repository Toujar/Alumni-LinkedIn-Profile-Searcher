package com.example.alumni.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response from GET /api/v2/containers/fetch-result-object?id={containerId}.
 *
 * The "resultObject" field is the raw JSON produced by the Phantom.
 * For the LinkedIn Search Export it is a JSON array of profile objects.
 * We keep it as a {@link JsonNode} so the client layer can decide how to
 * parse it — this avoids tight coupling between the HTTP layer and the
 * domain model.
 *
 * The "status" field reflects the container execution state:
 *   "running"  – still in progress
 *   "finished" – completed successfully
 *   "error"    – the Phantom failed
 *
 * All unknown top-level fields are ignored for forward-compatibility.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhantomBusterResultResponse {

    private String status;
    private JsonNode resultObject;
}
