package ru.hh.match.presentation.controller;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.hh.match.application.port.in.StartMatchingUseCase;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.domain.repository.MatchResultRepository;
import ru.hh.match.presentation.dto.response.ApiResponse;
import ru.hh.match.presentation.dto.response.MatchResultResponse;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private final StartMatchingUseCase startMatchingUseCase;
    private final MatchResultRepository matchResultRepository;
    private final CachePort cachePort;
    private final SseController sseController;
    private final JsonMapper objectMapper;

    public MatchingController(StartMatchingUseCase startMatchingUseCase,
                              MatchResultRepository matchResultRepository,
                              CachePort cachePort,
                              SseController sseController,
                              JsonMapper objectMapper) {
        this.startMatchingUseCase = startMatchingUseCase;
        this.matchResultRepository = matchResultRepository;
        this.cachePort = cachePort;
        this.sseController = sseController;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> startMatching(
            @CookieValue("session_id") UUID sessionId) {
        int sentCount = startMatchingUseCase.startMatching(sessionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(Map.of("sentRequests", sentCount)));
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<MatchResultResponse>>> getResults(
            @CookieValue("session_id") UUID sessionId) {
        List<MatchResult> results = matchResultRepository.findBySessionId(sessionId);
        List<MatchResultResponse> responses = results.stream()
                .map(r -> new MatchResultResponse(
                        r.getId(),
                        r.getResume().getId(),
                        r.getVacancy().getId(),
                        r.getVacancy().getJobTitle(),
                        r.getScore(),
                        r.getStatus().name()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResults(@RequestParam UUID sessionId) {
        log.info("SSE stream requested for session {}", sessionId);
        return sseController.createEmitter(sessionId);
    }
}
