package ru.hh.match.presentation.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.hh.match.domain.exception.HhApiException;
import ru.hh.match.domain.exception.HhTokenExpiredException;
import ru.hh.match.domain.exception.MatchingException;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.presentation.dto.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionNotFound(SessionNotFoundException ex) {
        log.error("Session not found: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResumeNotFound(ResumeNotFoundException ex) {
        log.error("Resume not found: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(HhTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleHhTokenExpired(HhTokenExpiredException ex) {
        log.error("HH API token expired: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("HH API token expired, re-authentication required"));
    }

    @ExceptionHandler(HhApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleHhApiException(HhApiException ex) {
        log.error("HH API error: {}", ex.getMessage(), ex);
        if (ex.getMessage() != null && ex.getMessage().contains("rate limit")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.fail(ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(MatchingException.class)
    public ResponseEntity<ApiResponse<Void>> handleMatchingException(MatchingException ex) {
        log.error("Matching error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Internal server error"));
    }
}
