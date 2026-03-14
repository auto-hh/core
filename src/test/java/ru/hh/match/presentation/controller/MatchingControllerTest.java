package ru.hh.match.presentation.controller;

import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.hh.match.application.port.in.StartMatchingUseCase;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.domain.repository.MatchResultRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingControllerTest {

    @Mock private StartMatchingUseCase startMatchingUseCase;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private CachePort cachePort;
    @Mock private SseController sseController;
    @Mock private JsonMapper jsonMapper;

    @InjectMocks private MatchingController matchingController;

    @Test
    void startMatching_shouldReturn202() {
        UUID sessionId = UUID.randomUUID();
        when(startMatchingUseCase.startMatching(sessionId)).thenReturn(10);

        var response = matchingController.startMatching(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().get("sentRequests")).isEqualTo(10);
    }

    @Test
    void streamResults_shouldReturnSseEmitter() {
        UUID sessionId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        when(sseController.createEmitter(sessionId)).thenReturn(emitter);

        SseEmitter result = matchingController.streamResults(sessionId);

        assertThat(result).isEqualTo(emitter);
    }
}
