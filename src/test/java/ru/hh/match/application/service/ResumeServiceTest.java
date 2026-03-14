package ru.hh.match.application.service;

import tools.jackson.databind.json.JsonMapper;
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
import ru.hh.match.application.port.out.HhResumePort;
import ru.hh.match.application.port.out.SessionPort;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.domain.repository.ResumeRepository;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;
import ru.hh.match.infrastructure.config.AppProperties;
import ru.hh.match.infrastructure.mapper.ResumeMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private HhResumePort hhResumePort;
    @Mock private SessionPort sessionPort;
    @Mock private ResumeRepository resumeRepository;
    @Mock private CachePort cachePort;
    @Mock private ResumeMapper resumeMapper;
    @Mock private AppProperties appProperties;
    @Spy private JsonMapper jsonMapper = JsonMapper.builder().build();

    @InjectMocks private ResumeService resumeService;

    @Test
    void syncResume_firstVisit_shouldFetchAndSave() {
        UUID sessionId = UUID.randomUUID();
        String accessToken = "test-token";

        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of(accessToken));
        when(cachePort.exists("session:seen:" + sessionId)).thenReturn(false);

        HhResumeDto resumeDto = new HhResumeDto(
                "resume-1", "Java Developer", new HhResumeDto.Area("Moscow"),
                new HhResumeDto.Salary(200000, "RUB"),
                List.of("Java", "Spring"), "About me",
                new HhResumeDto.TotalExperience(36),
                List.of(new HhResumeDto.Experience("Developer", "Company")),
                new HhResumeDto.Education(List.of(new HhResumeDto.Education.Primary("MSU", 2019)))
        );

        Resume resume = new Resume();
        resume.setHhResumeId("resume-1");
        resume.setJobTitle("Java Developer");
        resume.setSessionId(sessionId);

        when(hhResumePort.fetchFirstResume(accessToken)).thenReturn(resumeDto);
        when(resumeMapper.toEntity(resumeDto)).thenReturn(resume);
        when(resumeRepository.findByHhResumeId("resume-1")).thenReturn(Optional.empty());
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        var cacheConfig = new AppProperties.CacheConfig(3600, 1800);
        when(appProperties.cache()).thenReturn(cacheConfig);

        Resume result = resumeService.syncResume(sessionId);

        assertThat(result.getJobTitle()).isEqualTo("Java Developer");
        verify(resumeRepository).save(any(Resume.class));
        verify(cachePort).set(eq("session:seen:" + sessionId), any(), anyLong());
    }

    @Test
    void syncResume_noToken_shouldThrow() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.syncResume(sessionId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void syncResume_existingSession_shouldReturnFromCacheOrDb() {
        UUID sessionId = UUID.randomUUID();
        String accessToken = "test-token";

        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of(accessToken));
        when(cachePort.exists("session:seen:" + sessionId)).thenReturn(true);
        when(cachePort.get("cache:resume:" + sessionId)).thenReturn(Optional.empty());

        Resume resume = new Resume();
        resume.setJobTitle("Java Developer");
        when(resumeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(resume));

        Resume result = resumeService.syncResume(sessionId);

        assertThat(result.getJobTitle()).isEqualTo("Java Developer");
    }
}
