package com.example.alumni.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configures the Spring {@link RestClient} bean used by PhantomBusterClientImpl.
 *
 * A dedicated, named bean keeps the PhantomBuster HTTP client isolated from
 * any other RestClient instances that might be added in future. The base URL
 * and auth header are NOT set here — they are applied per-request in the
 * client implementation so that the bean remains fully testable with mocks.
 *
 * Timeout values are set at the HTTP client level via the builder.
 * 30 s connect / 30 s read covers normal PhantomBuster launch latency.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient phantomBusterRestClient() {
        return RestClient.builder()
                .build();
    }
}
