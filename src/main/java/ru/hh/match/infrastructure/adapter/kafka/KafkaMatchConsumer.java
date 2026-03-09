package ru.hh.match.infrastructure.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.domain.model.enums.MatchStatus;
import ru.hh.match.domain.repository.MatchResultRepository;
import ru.hh.match.infrastructure.adapter.kafka.dto.MatchResponseMessage;
import ru.hh.match.infrastructure.config.AppProperties;
import ru.hh.match.presentation.controller.SseController;

@Component
public class KafkaMatchConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaMatchConsumer.class);

    private final MatchResultRepository matchResultRepository;
    private final CachePort cachePort;
    private final SseController sseController;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public KafkaMatchConsumer(MatchResultRepository matchResultRepository,
                              CachePort cachePort,
                              SseController sseController,
                              AppProperties appProperties,
                              ObjectMapper objectMapper) {
        this.matchResultRepository = matchResultRepository;
        this.cachePort = cachePort;
        this.sseController = sseController;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.match-response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleMatchResponse(MatchResponseMessage message) {
        log.info("Received match response, correlationId: {}, score: {}", message.correlationId(), message.score());

        try {
            matchResultRepository.findByResumeIdAndVacancyId(message.resumeId(), message.vacancyId())
                    .ifPresent(matchResult -> {
                        MatchStatus status = "COMPLETED".equals(message.status())
                                ? MatchStatus.COMPLETED : MatchStatus.FAILED;
                        matchResult.setScore(message.score());
                        matchResult.setStatus(status);
                        matchResultRepository.save(matchResult);

                        updateCache(message.sessionId());

                        sseController.sendMatchResult(
                                UUID.fromString(message.sessionId()),
                                matchResult
                        );

                        checkAndCompleteStream(UUID.fromString(message.sessionId()));
                    });
        } catch (Exception e) {
            log.error("Error processing match response for correlationId: {}", message.correlationId(), e);
        }
    }

    private void updateCache(String sessionId) {
        try {
            List<MatchResult> results = matchResultRepository.findBySessionId(UUID.fromString(sessionId));
            String json = objectMapper.writeValueAsString(results);
            cachePort.set("cache:match:" + sessionId, json, appProperties.cache().matchResultsTtl());
        } catch (JsonProcessingException e) {
            log.warn("Failed to update match results cache for session {}", sessionId, e);
        }
    }

    private void checkAndCompleteStream(UUID sessionId) {
        List<MatchResult> pending = matchResultRepository.findBySessionIdAndStatus(sessionId, MatchStatus.PENDING);
        if (pending.isEmpty()) {
            List<MatchResult> all = matchResultRepository.findBySessionId(sessionId);
            long completed = all.stream().filter(r -> r.getStatus() == MatchStatus.COMPLETED).count();
            sseController.sendComplete(sessionId, all.size(), (int) completed);
        }
    }
}
