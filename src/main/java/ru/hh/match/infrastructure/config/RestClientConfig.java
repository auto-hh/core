package ru.hh.match.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient hhRestClient(AppProperties appProperties) {
        return RestClient.builder()
                .baseUrl(appProperties.hhApi().baseUrl())
                .defaultHeader("User-Agent", appProperties.hhApi().userAgent())
                .build();
    }
}
