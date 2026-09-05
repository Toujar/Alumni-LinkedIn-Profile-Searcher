package com.example.alumni.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configures the Spring {@link RestClient} bean used by PhantomBusterClientImpl.
 *
 * A 30-second connect timeout and 30-second read timeout are applied via
 * {@link SimpleClientHttpRequestFactory}. These cover the initial TCP handshake
 * and any individual HTTP read operation. The poll loop in PhantomBusterClientImpl
 * provides a higher-level logical timeout for the full Phantom run.
 *
 * A dedicated named bean keeps the PhantomBuster HTTP client isolated from
 * any other RestClient instances that might be added in future.
 */
@Configuration
public class RestClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS    = 30_000;

    @Bean
    public RestClient phantomBusterRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
