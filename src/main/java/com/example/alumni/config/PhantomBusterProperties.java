package com.example.alumni.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed configuration for PhantomBuster integration.
 *
 * All values are resolved from environment variables via application.properties:
 *
 *   phantombuster.api-key          → PHANTOMBUSTER_API_KEY
 *   phantombuster.base-url         → PHANTOMBUSTER_BASE_URL
 *   phantombuster.agent-id         → PHANTOMBUSTER_AGENT_ID
 *   phantombuster.poll-interval-ms → PHANTOMBUSTER_POLL_INTERVAL_MS (default 3000)
 *   phantombuster.poll-max-attempts→ PHANTOMBUSTER_POLL_MAX_ATTEMPTS (default 20)
 *
 * The Phantom (LinkedIn Search Export) must be pre-configured in the
 * PhantomBuster dashboard with a valid LinkedIn session cookie before
 * the application can call it successfully.
 */
@Validated
@ConfigurationProperties(prefix = "phantombuster")
public class PhantomBusterProperties {

    @NotBlank(message = "PhantomBuster API key must be configured")
    private String apiKey;

    @NotBlank(message = "PhantomBuster base URL must be configured")
    private String baseUrl;

    /**
     * The Agent ID of the pre-configured LinkedIn Search Export Phantom.
     * Find this in the PhantomBuster dashboard URL or API tab.
     */
    @NotBlank(message = "PhantomBuster agent ID must be configured")
    private String agentId;

    /**
     * Milliseconds to wait between polling attempts while the Phantom runs.
     * Default: 3000 ms. Increase if the Phantom consistently takes longer.
     */
    @Positive
    private long pollIntervalMs = 3_000;

    /**
     * Maximum number of polling attempts before giving up.
     * Default: 20 → 60 seconds total at 3 s interval.
     * Raise this for Phantoms that process large result sets.
     */
    @Positive
    private int pollMaxAttempts = 20;

    // ---------- getters & setters (no Lombok to keep @Validated working cleanly) ----------

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getPollMaxAttempts() { return pollMaxAttempts; }
    public void setPollMaxAttempts(int pollMaxAttempts) { this.pollMaxAttempts = pollMaxAttempts; }
}
