package ru.hh.match.presentation.controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.infrastructure.config.AppProperties;

@Component
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public SseController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public SseEmitter createEmitter(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(appProperties.sse().timeoutMs());

        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));

        emitters.put(sessionId, emitter);
        return emitter;
    }

    public void sendMatchResult(UUID sessionId, MatchResult matchResult) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            log.debug("No SSE emitter found for session {}, result saved to DB only", sessionId);
            return;
        }

        try {
            Map<String, Object> data = Map.of(
                    "vacancyId", matchResult.getVacancy().getId(),
                    "jobTitle", matchResult.getVacancy().getJobTitle(),
                    "score", matchResult.getScore() != null ? matchResult.getScore() : 0.0
            );
            emitter.send(SseEmitter.event()
                    .name("match-result")
                    .data(data));
        } catch (IOException e) {
            log.warn("Failed to send SSE event for session {}", sessionId, e);
            emitters.remove(sessionId);
        }
    }

    public void sendComplete(UUID sessionId, int total, int completed) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(Map.of("total", total, "completed", completed)));
            emitter.complete();
        } catch (IOException e) {
            log.warn("Failed to send SSE complete event for session {}", sessionId, e);
        } finally {
            emitters.remove(sessionId);
        }
    }
}
