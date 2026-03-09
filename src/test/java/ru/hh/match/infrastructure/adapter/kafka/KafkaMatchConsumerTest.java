package ru.hh.match.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.domain.model.enums.MatchStatus;
import ru.hh.match.domain.repository.MatchResultRepository;
import ru.hh.match.infrastructure.adapter.kafka.dto.MatchResponseMessage;
import ru.hh.match.infrastructure.config.AppProperties;
import ru.hh.match.presentation.controller.SseController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMatchConsumerTest {

    @Mock private MatchResultRepository matchResultRepository;
    @Mock private CachePort cachePort;
    @Mock private SseController sseController;
    @Mock private AppProperties appProperties;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private KafkaMatchConsumer kafkaMatchConsumer;

    @Test
    void handleMatchResponse_shouldUpdateMatchResult() {
        UUID sessionId = UUID.randomUUID();
        MatchResponseMessage message = new MatchResponseMessage(
                "corr-1", sessionId.toString(), 1L, 2L, 0.85, "COMPLETED"
        );

        MatchResult matchResult = new MatchResult();
        matchResult.setStatus(MatchStatus.PENDING);
        Vacancy vacancy = new Vacancy();
        vacancy.setId(2L);
        vacancy.setJobTitle("Dev");
        matchResult.setVacancy(vacancy);

        when(matchResultRepository.findByResumeIdAndVacancyId(1L, 2L)).thenReturn(Optional.of(matchResult));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);
        when(matchResultRepository.findBySessionId(any(UUID.class))).thenReturn(List.of(matchResult));
        when(matchResultRepository.findBySessionIdAndStatus(any(UUID.class), eq(MatchStatus.PENDING)))
                .thenReturn(List.of());

        var cacheConfig = new AppProperties.CacheConfig(3600, 1800);
        when(appProperties.cache()).thenReturn(cacheConfig);

        kafkaMatchConsumer.handleMatchResponse(message);

        verify(matchResultRepository).save(matchResult);
        assertThat(matchResult.getScore()).isEqualTo(0.85);
        assertThat(matchResult.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }
}
