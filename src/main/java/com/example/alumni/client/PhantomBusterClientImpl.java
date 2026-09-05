package com.example.alumni.client;

import com.example.alumni.config.PhantomBusterProperties;
import com.example.alumni.dto.external.LinkedInProfileResult;
import com.example.alumni.dto.external.PhantomBusterLaunchRequest;
import com.example.alumni.dto.external.PhantomBusterLaunchResponse;
import com.example.alumni.dto.external.PhantomBusterResultResponse;
import com.example.alumni.dto.request.AlumniSearchRequest;
import com.example.alumni.exception.PhantomBusterException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Implements {@link PhantomBusterClient} using the PhantomBuster v2 REST API
 * and Spring's synchronous {@link RestClient}.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Build a LinkedIn People search URL from the request parameters.</li>
 *   <li>POST to {@code /api/v2/agents/launch} → receive a {@code containerId}.</li>
 *   <li>Poll {@code GET /api/v2/containers/fetch-result-object?id={containerId}}
 *       until the container status is {@code "finished"} or {@code "error"}.</li>
 *   <li>Parse the {@code resultObject} JSON array into {@link LinkedInProfileResult}
 *       instances and return them.</li>
 * </ol>
 *
 * <h2>LinkedIn search URL construction</h2>
 * PhantomBuster's LinkedIn Search Export accepts a standard LinkedIn people-search
 * URL as its {@code linkedInSearchUrl} argument. We build this URL by combining
 * the designation and university as free-text keywords. The Phantom then scrapes
 * the results exactly as a user would see them on LinkedIn.
 *
 * <h2>Authentication</h2>
 * Every request carries the {@code X-Phantombuster-Key-1} header whose value is
 * read from {@link PhantomBusterProperties#getApiKey()}. The key is never logged.
 *
 * <h2>Error handling</h2>
 * All HTTP and parsing errors are translated into {@link PhantomBusterException}
 * so callers have a single exception type to handle.
 */
@Component
public class PhantomBusterClientImpl implements PhantomBusterClient {

    private static final Logger log = LoggerFactory.getLogger(PhantomBusterClientImpl.class);

    private static final String AUTH_HEADER = "X-Phantombuster-Key-1";
    private static final String LAUNCH_PATH = "/agents/launch";
    private static final String RESULT_PATH = "/containers/fetch-result-object";

    /** Container execution states returned by PhantomBuster. */
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_ERROR    = "error";

    /**
     * Base LinkedIn people-search URL. Keywords are appended as a query parameter.
     * Using {@code /search/results/people/} scopes results to people only.
     */
    private static final String LINKEDIN_PEOPLE_SEARCH_BASE =
            "https://www.linkedin.com/search/results/people/?keywords=";

    private final RestClient restClient;
    private final PhantomBusterProperties properties;
    private final ObjectMapper objectMapper;

    public PhantomBusterClientImpl(RestClient phantomBusterRestClient,
                                   PhantomBusterProperties properties,
                                   ObjectMapper objectMapper) {
        this.restClient   = phantomBusterRestClient;
        this.properties   = properties;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public List<LinkedInProfileResult> searchAlumni(AlumniSearchRequest request) {
        log.info("Launching PhantomBuster LinkedIn Search Export for university='{}', designation='{}'",
                request.getUniversity(), request.getDesignation());

        String linkedInSearchUrl = buildLinkedInSearchUrl(request);
        String containerId       = launchPhantom(linkedInSearchUrl);

        log.info("PhantomBuster Phantom launched. containerId={}", containerId);

        PhantomBusterResultResponse result = pollUntilComplete(containerId);
        return parseProfiles(result);
    }

    // -------------------------------------------------------------------------
    // Step 1 – build LinkedIn search URL
    // -------------------------------------------------------------------------

    /**
     * Constructs a LinkedIn people-search URL from the request parameters.
     *
     * Format: https://www.linkedin.com/search/results/people/?keywords={designation}+{university}
     *
     * Both terms are URL-encoded. The combination reliably surfaces alumni
     * profiles that mention both the company/role and the institution.
     */
    private String buildLinkedInSearchUrl(AlumniSearchRequest request) {
        String keywords = request.getDesignation() + " " + request.getUniversity();
        String encoded  = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        return LINKEDIN_PEOPLE_SEARCH_BASE + encoded;
    }

    // -------------------------------------------------------------------------
    // Step 2 – launch the Phantom
    // -------------------------------------------------------------------------

    private String launchPhantom(String linkedInSearchUrl) {
        String argument = buildArgumentJson(linkedInSearchUrl);

        PhantomBusterLaunchRequest launchRequest = PhantomBusterLaunchRequest.builder()
                .agentId(properties.getAgentId())
                .argument(argument)
                .build();

        try {
            PhantomBusterLaunchResponse response = restClient.post()
                    .uri(properties.getBaseUrl() + LAUNCH_PATH)
                    .header(AUTH_HEADER, properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(launchRequest)
                    .retrieve()
                    .body(PhantomBusterLaunchResponse.class);

            if (response == null || response.getContainerId() == null) {
                throw new PhantomBusterException(
                        "PhantomBuster launch returned an empty response or missing containerId");
            }

            return response.getContainerId();

        } catch (HttpClientErrorException ex) {
            throw new PhantomBusterException(
                    "PhantomBuster rejected the launch request (HTTP " + ex.getStatusCode() + "): "
                    + ex.getResponseBodyAsString(), ex);

        } catch (HttpServerErrorException ex) {
            throw new PhantomBusterException(
                    "PhantomBuster server error during launch (HTTP " + ex.getStatusCode() + ")", ex);

        } catch (ResourceAccessException ex) {
            throw new PhantomBusterException(
                    "Could not connect to PhantomBuster API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Serialises the LinkedIn search URL into the JSON string that
     * PhantomBuster's {@code argument} field expects.
     *
     * Result: {"linkedInSearchUrl":"https://www.linkedin.com/search/..."}
     */
    private String buildArgumentJson(String linkedInSearchUrl) {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of("linkedInSearchUrl", linkedInSearchUrl));
        } catch (JsonProcessingException ex) {
            // Should never happen for a plain String map — defensive guard
            throw new PhantomBusterException("Failed to serialise PhantomBuster argument", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Step 3 – poll until the container finishes
    // -------------------------------------------------------------------------

    private PhantomBusterResultResponse pollUntilComplete(String containerId) {
        int attempts = 0;

        while (attempts < properties.getPollMaxAttempts()) {
            attempts++;

            PhantomBusterResultResponse response = fetchContainerResult(containerId);
            String status = response.getStatus();

            log.debug("Poll attempt {}/{} for containerId={} — status={}",
                    attempts, properties.getPollMaxAttempts(), containerId, status);

            if (STATUS_FINISHED.equalsIgnoreCase(status)) {
                log.info("Phantom run finished. containerId={}", containerId);
                return response;
            }

            if (STATUS_ERROR.equalsIgnoreCase(status)) {
                throw new PhantomBusterException(
                        "PhantomBuster Phantom run failed. containerId=" + containerId);
            }

            // Still running — wait before the next poll
            sleep(properties.getPollIntervalMs());
        }

        throw new PhantomBusterException(
                "PhantomBuster Phantom did not complete within the allowed time. "
                + "containerId=" + containerId
                + ", maxAttempts=" + properties.getPollMaxAttempts());
    }

    private PhantomBusterResultResponse fetchContainerResult(String containerId) {
        try {
            PhantomBusterResultResponse response = restClient.get()
                    .uri(properties.getBaseUrl() + RESULT_PATH + "?id=" + containerId)
                    .header(AUTH_HEADER, properties.getApiKey())
                    .retrieve()
                    .body(PhantomBusterResultResponse.class);

            if (response == null) {
                throw new PhantomBusterException(
                        "Received empty response while polling containerId=" + containerId);
            }

            return response;

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new PhantomBusterException(
                        "Container not found on PhantomBuster. containerId=" + containerId, ex);
            }
            throw new PhantomBusterException(
                    "PhantomBuster returned client error while polling (HTTP "
                    + ex.getStatusCode() + ")", ex);

        } catch (HttpServerErrorException ex) {
            throw new PhantomBusterException(
                    "PhantomBuster server error while polling result (HTTP "
                    + ex.getStatusCode() + ")", ex);

        } catch (ResourceAccessException ex) {
            throw new PhantomBusterException(
                    "Connection error while polling PhantomBuster: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Step 4 – parse profiles from result
    // -------------------------------------------------------------------------

    private List<LinkedInProfileResult> parseProfiles(PhantomBusterResultResponse response) {
        JsonNode resultObject = response.getResultObject();

        if (resultObject == null || resultObject.isNull()) {
            log.info("PhantomBuster returned no resultObject — empty result set.");
            return Collections.emptyList();
        }

        if (!resultObject.isArray()) {
            log.warn("PhantomBuster resultObject is not an array. Returning empty list.");
            return Collections.emptyList();
        }

        try {
            List<LinkedInProfileResult> profiles = objectMapper.convertValue(
                    resultObject,
                    new TypeReference<List<LinkedInProfileResult>>() {}
            );
            log.info("Parsed {} profile(s) from PhantomBuster result.", profiles.size());
            return profiles;

        } catch (IllegalArgumentException ex) {
            throw new PhantomBusterException(
                    "Failed to parse PhantomBuster result into profile list", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PhantomBusterException(
                    "Polling interrupted while waiting for PhantomBuster result", ex);
        }
    }
}
