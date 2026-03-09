package ru.hh.match.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.application.port.out.HhVacancyPort;
import ru.hh.match.application.port.out.SessionPort;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.domain.repository.ResumeRepository;
import ru.hh.match.domain.repository.VacancyRepository;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;
import ru.hh.match.infrastructure.config.AppProperties;
import ru.hh.match.infrastructure.mapper.VacancyMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock private HhVacancyPort hhVacancyPort;
    @Mock private SessionPort sessionPort;
    @Mock private ResumeRepository resumeRepository;
    @Mock private VacancyRepository vacancyRepository;
    @Mock private VacancyMapper vacancyMapper;
    @Mock private AppProperties appProperties;
    @Mock private CachePort cachePort;

    @InjectMocks private VacancyService vacancyService;

    @Test
    void searchVacancies_shouldReturnVacancies() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of("token"));

        Resume resume = new Resume();
        resume.setJobTitle("Java Developer");
        resume.setSkillsRes("Java, Spring");
        when(resumeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(resume));

        var vacancyConfig = new AppProperties.VacancyConfig(50, List.of("title", "skills"));
        when(appProperties.vacancy()).thenReturn(vacancyConfig);

        HhVacancyDto vacancyDto = new HhVacancyDto(
                "v1", "Senior Java Dev", "Description",
                new HhVacancyDto.VacancyExperience("between3And6", "3-6 years"),
                new HhVacancyDto.Salary(200000, 300000, "RUR"),
                List.of(new HhVacancyDto.KeySkill("Java")),
                List.of(new HhVacancyDto.ProfessionalRole("Backend"))
        );
        when(hhVacancyPort.searchVacancies(anyString(), anyString(), anyInt())).thenReturn(List.of(vacancyDto));

        Vacancy vacancy = new Vacancy();
        vacancy.setId(1L);
        vacancy.setHhVacancyId("v1");
        when(vacancyRepository.findByHhVacancyId("v1")).thenReturn(Optional.empty());
        when(vacancyMapper.toEntity(vacancyDto)).thenReturn(vacancy);
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(vacancy);

        List<Vacancy> result = vacancyService.searchVacancies(sessionId);

        assertThat(result).hasSize(1);
        verify(vacancyRepository).save(any(Vacancy.class));
    }

    @Test
    void searchVacancies_noSession_shouldThrow() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacancyService.searchVacancies(sessionId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void searchVacancies_noResume_shouldThrow() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of("token"));
        when(resumeRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacancyService.searchVacancies(sessionId))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    void searchVacancies_existingVacancy_shouldNotDuplicate() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of("token"));

        Resume resume = new Resume();
        resume.setJobTitle("Dev");
        resume.setSkillsRes("");
        when(resumeRepository.findBySessionId(sessionId)).thenReturn(Optional.of(resume));

        var vacancyConfig = new AppProperties.VacancyConfig(50, List.of("title"));
        when(appProperties.vacancy()).thenReturn(vacancyConfig);

        HhVacancyDto dto = new HhVacancyDto("v1", "Dev", null, null, null, null, null);
        when(hhVacancyPort.searchVacancies(anyString(), anyString(), anyInt())).thenReturn(List.of(dto));

        Vacancy existing = new Vacancy();
        existing.setId(1L);
        when(vacancyRepository.findByHhVacancyId("v1")).thenReturn(Optional.of(existing));

        List<Vacancy> result = vacancyService.searchVacancies(sessionId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }
}
