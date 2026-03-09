package ru.hh.match.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    HhApi hhApi,
    VacancyConfig vacancy,
    KafkaTopics kafka,
    CacheConfig cache,
    SseConfig sse
) {
    public record HhApi(
        String baseUrl,
        String userAgent,
        RateLimit rateLimit,
        Endpoints endpoints
    ) {
        public record RateLimit(int maxRequestsPerSecond, int retryMaxAttempts, long retryDelayMs) {}
        public record Endpoints(String resumes, String vacancySearch, String vacancyDetail) {}
    }

    public record VacancyConfig(int searchLimit, List<String> relevanceFields) {}
    public record KafkaTopics(Topics topics, int batchSize) {
        public record Topics(String matchRequest, String matchResponse) {}
    }
    public record CacheConfig(long matchResultsTtl, long resumeTtl) {}
    public record SseConfig(long timeoutMs) {}
}
