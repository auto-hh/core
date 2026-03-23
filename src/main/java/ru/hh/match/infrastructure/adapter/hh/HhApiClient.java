package ru.hh.match.infrastructure.adapter.hh;

import java.net.URI;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.hh.match.domain.exception.HhApiException;
import ru.hh.match.domain.exception.HhTokenExpiredException;
import ru.hh.match.infrastructure.config.AppProperties;

@Component
public class HhApiClient {

    private static final Logger log = LoggerFactory.getLogger(HhApiClient.class);

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final Semaphore rateLimiter;

    public HhApiClient(RestClient hhRestClient, AppProperties appProperties) {
        this.restClient = hhRestClient;
        this.appProperties = appProperties;
        this.rateLimiter = new Semaphore(appProperties.hhApi().rateLimit().maxRequestsPerSecond());
    }

    public <T> T get(String path, String accessToken, Class<T> responseType) {
        int maxAttempts = appProperties.hhApi().rateLimit().retryMaxAttempts();
        long retryDelay = appProperties.hhApi().rateLimit().retryDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                acquireRateLimit();

                log.debug("HH API request: GET {} | Token: {}... | User-Agent: {}",
                        path,
                        accessToken.length() > 10 ? accessToken.substring(0, 10) : accessToken,
                        appProperties.hhApi().userAgent());

                long start = System.currentTimeMillis();
                T result = restClient.get()
                        .uri(path)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .body(responseType);

                long elapsed = System.currentTimeMillis() - start;
                log.info("HH API GET {} - {}ms", path, elapsed);

                return result;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("HH API rate limited (attempt {}/{}), retrying in {}ms", attempt, maxAttempts, retryDelay);
                if (attempt == maxAttempts) {
                    throw new HhApiException("HH API rate limit exceeded after " + maxAttempts + " attempts", e);
                }
                sleep(retryDelay);
            } catch (HttpClientErrorException.Forbidden e) {
                log.error("HH API 403 on path [{}] | Response: {} | Token (first 10): {}... | User-Agent: {}",
                        path,
                        e.getResponseBodyAsString(),
                        accessToken.length() > 10 ? accessToken.substring(0, 10) : accessToken,
                        appProperties.hhApi().userAgent());
                // Diagnostic: try /me to check if token is valid at all
                boolean tokenValid = false;
                try {
                    String meResponse = restClient.get()
                            .uri("/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .body(String.class);
                    log.info("HH API /me succeeded (token IS valid): {}", meResponse != null ? meResponse.substring(0, Math.min(200, meResponse.length())) : "null");
                    tokenValid = true;
                } catch (Exception meEx) {
                    log.error("HH API /me also failed: {}", meEx.getMessage());
                }
                if (tokenValid) {
                    // Token works but this endpoint returned 403 — permission issue, not expired
                    throw new HhApiException("HH API 403 Forbidden on " + path + " (token is valid, likely permissions issue)", e);
                }
                throw new HhTokenExpiredException("HH API token expired or revoked", e);
            } catch (HttpClientErrorException e) {
                log.error("HH API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new HhApiException("HH API error: " + e.getStatusCode(), e);
            } catch (Exception e) {
                if (e instanceof HhApiException) throw (HhApiException) e;
                if (e instanceof HhTokenExpiredException) throw (HhTokenExpiredException) e;
                log.error("HH API unexpected error for {}", path, e);
                throw new HhApiException("HH API unexpected error", e);
            }
        }

        throw new HhApiException("HH API request failed after " + maxAttempts + " attempts");
    }

    public <T> T getAbsoluteUrl(String absoluteUrl, String accessToken, Class<T> responseType) {
        int maxAttempts = appProperties.hhApi().rateLimit().retryMaxAttempts();
        long retryDelay = appProperties.hhApi().rateLimit().retryDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                acquireRateLimit();

                log.debug("HH API request: GET {} (absolute) | Token: {}...",
                        absoluteUrl,
                        accessToken.length() > 10 ? accessToken.substring(0, 10) : accessToken);

                long start = System.currentTimeMillis();
                T result = restClient.get()
                        .uri(URI.create(absoluteUrl))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("User-Agent", appProperties.hhApi().userAgent())
                        .retrieve()
                        .body(responseType);

                long elapsed = System.currentTimeMillis() - start;
                log.info("HH API GET {} (absolute) - {}ms", absoluteUrl, elapsed);

                return result;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("HH API rate limited (attempt {}/{}), retrying in {}ms", attempt, maxAttempts, retryDelay);
                if (attempt == maxAttempts) {
                    throw new HhApiException("HH API rate limit exceeded after " + maxAttempts + " attempts", e);
                }
                sleep(retryDelay);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new HhTokenExpiredException("HH API token expired or invalid", e);
            } catch (HttpClientErrorException.Forbidden e) {
                log.error("HH API 403 on absolute URL [{}]", absoluteUrl);
                throw new HhApiException("HH API 403 Forbidden on " + absoluteUrl, e);
            } catch (HttpClientErrorException e) {
                log.error("HH API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new HhApiException("HH API error: " + e.getStatusCode(), e);
            } catch (Exception e) {
                if (e instanceof HhApiException) throw (HhApiException) e;
                if (e instanceof HhTokenExpiredException) throw (HhTokenExpiredException) e;
                log.error("HH API unexpected error for absolute URL {}", absoluteUrl, e);
                throw new HhApiException("HH API unexpected error", e);
            }
        }

        throw new HhApiException("HH API request failed after " + maxAttempts + " attempts");
    }

    private void acquireRateLimit() {
        try {
            if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                throw new HhApiException("Rate limit: could not acquire permit within timeout");
            }
            // Release after 1 second to maintain rate
            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                rateLimiter.release();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HhApiException("Rate limit interrupted", e);
        }
    }

    public <T> T getPublic(String path, Class<T> responseType) {
        int maxAttempts = appProperties.hhApi().rateLimit().retryMaxAttempts();
        long retryDelay = appProperties.hhApi().rateLimit().retryDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                acquireRateLimit();

                log.debug("HH API public request: GET {} | User-Agent: {}", path, appProperties.hhApi().userAgent());

                long start = System.currentTimeMillis();
                T result = restClient.get()
                        .uri(path)
                        .retrieve()
                        .body(responseType);

                long elapsed = System.currentTimeMillis() - start;
                log.info("HH API public GET {} - {}ms", path, elapsed);

                return result;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("HH API rate limited (attempt {}/{}), retrying in {}ms", attempt, maxAttempts, retryDelay);
                if (attempt == maxAttempts) {
                    throw new HhApiException("HH API rate limit exceeded after " + maxAttempts + " attempts", e);
                }
                sleep(retryDelay);
            } catch (HttpClientErrorException e) {
                log.error("HH API public error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new HhApiException("HH API error: " + e.getStatusCode(), e);
            } catch (Exception e) {
                if (e instanceof HhApiException) throw (HhApiException) e;
                log.error("HH API unexpected error for {}", path, e);
                throw new HhApiException("HH API unexpected error", e);
            }
        }

        throw new HhApiException("HH API public request failed after " + maxAttempts + " attempts");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HhApiException("Retry sleep interrupted", e);
        }
    }
}
