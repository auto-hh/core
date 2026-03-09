package ru.hh.match.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.match.application.port.out.MatchRequestPort;
import ru.hh.match.application.port.out.SessionPort;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.domain.model.enums.MatchStatus;
import ru.hh.match.domain.repository.MatchResultRepository;
import ru.hh.match.domain.repository.ResumeRepository;
import ru.hh.match.domain.repository.VacancyRepository;
import ru.hh.match.infrastructure.adapter.kafka.dto.MatchRequestMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock private SessionPort sessionPort;
    @Mock private ResumeRepository resumeRepository;
    @Mock private VacancyRepository vacancyRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MatchRequestPort matchRequestPort;

    @InjectMocks private MatchingService matchingService;

    @Test
    void startMatching_shouldSendRequests() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of("token"));

        Resume resume = new Resume();
        resume.setId(1L);
        resume.setSessionId(sessionId);
        resume.setJobTitle("Dev");
        when(resumeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(resume));

        Vacancy vacancy = new Vacancy();
        vacancy.setId(1L);
        vacancy.setJobTitle("Senior Dev");
        when(vacancyRepository.findAll()).thenReturn(List.of(vacancy));
        when(matchResultRepository.findByResumeIdAndVacancyId(1L, 1L)).thenReturn(Optional.empty());
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(inv -> inv.getArgument(0));

        int result = matchingService.startMatching(sessionId);

        assertThat(result).isEqualTo(1);
        verify(matchRequestPort).sendMatchRequest(any(MatchRequestMessage.class));
    }

    @Test
    void startMatching_completedMatch_shouldSkip() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of("token"));

        Resume resume = new Resume();
        resume.setId(1L);
        when(resumeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(resume));

        Vacancy vacancy = new Vacancy();
        vacancy.setId(1L);
        when(vacancyRepository.findAll()).thenReturn(List.of(vacancy));

        MatchResult existing = new MatchResult();
        existing.setStatus(MatchStatus.COMPLETED);
        when(matchResultRepository.findByResumeIdAndVacancyId(1L, 1L)).thenReturn(Optional.of(existing));

        int result = matchingService.startMatching(sessionId);

        assertThat(result).isEqualTo(0);
        verify(matchRequestPort, never()).sendMatchRequest(any());
    }

    @Test
    void startMatching_noSession_shouldThrow() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.startMatching(sessionId))
                .isInstanceOf(SessionNotFoundException.class);
    }
}
