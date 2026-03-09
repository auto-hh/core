package ru.hh.match.presentation.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.hh.match.domain.exception.HhApiException;
import ru.hh.match.domain.exception.MatchingException;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.presentation.dto.response.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSessionNotFound_shouldReturn401() {
        var response = handler.handleSessionNotFound(new SessionNotFoundException("Session not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error()).isEqualTo("Session not found");
    }

    @Test
    void handleResumeNotFound_shouldReturn404() {
        var response = handler.handleResumeNotFound(new ResumeNotFoundException("No resume"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    void handleHhApiException_shouldReturn502() {
        var response = handler.handleHhApiException(new HhApiException("HH API error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    void handleHhApiException_rateLimited_shouldReturn429() {
        var response = handler.handleHhApiException(new HhApiException("HH API rate limit exceeded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void handleMatchingException_shouldReturn500() {
        var response = handler.handleMatchingException(new MatchingException("Matching error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    void handleGenericException_shouldReturn500() {
        var response = handler.handleGenericException(new RuntimeException("Unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("Internal server error");
    }
}
